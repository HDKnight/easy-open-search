package org.dromara.easyos.jdbc;

import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JdbcExecutor {
    private static final Logger log = LoggerFactory.getLogger(JdbcExecutor.class);

    private final EasyOsProperties properties;
    private volatile boolean probed;

    public JdbcExecutor(EasyOsProperties properties) {
        this.properties = properties;
    }

    public Connection openConnection() throws SQLException {
        String url = JdbcUrlBuilder.build(properties);
        Properties props = new Properties();
        if (properties.getUsername() != null) {
            props.setProperty("user", properties.getUsername());
        }
        if (properties.getPassword() != null) {
            props.setProperty("password", properties.getPassword());
        }
        if (properties.isTrustSelfSigned()) {
            // sql-jdbc: accept demo/self-signed HTTPS certs (dev only)
            props.setProperty("trustSelfSigned", "true");
            props.setProperty("hostnameVerification", "false");
        }
        try {
            Class.forName("org.opensearch.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new EasyOsException("OpenSearch JDBC driver not found on classpath", e);
        }
        return DriverManager.getConnection(url, props);
    }

    public void probe() {
        String jdbcUrl = JdbcUrlBuilder.build(properties);
        log.info("[easy-os] 探测 OpenSearch SQL 连通性: {}", jdbcUrl);
        try (Connection connection = openConnection()) {
            SqlProbe.verify(connection);
            probed = true;
            log.info("[easy-os] OpenSearch SQL 探测成功");
        } catch (EasyOsException e) {
            log.error("[easy-os] OpenSearch SQL 探测失败（请确认已安装 opensearch-sql 插件）", e);
            throw e;
        } catch (Exception e) {
            log.error("[easy-os] OpenSearch SQL 探测失败: {}", e.getMessage());
            throw translate(e);
        }
    }

    private void ensureProbed() {
        if (!probed) {
            synchronized (this) {
                if (!probed) {
                    probe();
                }
            }
        }
    }

    public List<Map<String, Object>> queryMaps(org.dromara.easyos.sql.BoundSql boundSql) {
        ensureProbed();
        long startNs = System.nanoTime();
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(boundSql.getSql())) {
            List<Object> params = boundSql.getParams();
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = ResultSetMapper.toMaps(rs);
                if (properties.isPrintSql()) {
                    log.info("[easy-os] SQL: {} | params={} | cost={}ms | rows={}",
                            boundSql.getSql(), boundSql.getParams(), elapsedMs(startNs), rows.size());
                }
                return rows;
            }
        } catch (Exception e) {
            if (properties.isPrintSql()) {
                log.warn("[easy-os] SQL failed: {} | params={} | cost={}ms",
                        boundSql.getSql(), boundSql.getParams(), elapsedMs(startNs));
            }
            throw translate(e);
        }
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    public Long queryLong(org.dromara.easyos.sql.BoundSql boundSql) {
        List<Map<String, Object>> rows = queryMaps(boundSql);
        if (rows.isEmpty()) {
            return 0L;
        }
        Object first = rows.get(0).values().iterator().next();
        if (first == null) {
            return 0L;
        }
        if (first instanceof Number) {
            return ((Number) first).longValue();
        }
        return Long.parseLong(String.valueOf(first));
    }

    private RuntimeException translate(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("pkix") || msg.contains("sslhandshake") || msg.contains("certpath")) {
            log.error("[easy-os] HTTPS 证书校验失败。开发环境请设置 easy-open-search.trust-self-signed=true");
            return new EasyOsException(
                    "OpenSearch JDBC SSL failed (self-signed?). Set easy-open-search.trust-self-signed=true", e);
        }
        if (msg.contains("sql") && (msg.contains("plugin") || msg.contains("404") || msg.contains("not found"))) {
            log.error("[easy-os] 疑似缺少 SQL 插件，请检查: bin/opensearch-plugin list / install opensearch-sql");
            return EasyOsException.sqlPluginMissing();
        }
        if (e instanceof EasyOsException) {
            return (EasyOsException) e;
        }
        log.error("[easy-os] JDBC 执行失败: {}", e.getMessage());
        return new EasyOsException("OpenSearch JDBC execution failed: " + e.getMessage(), e);
    }
}
