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
        try {
            Class.forName("org.opensearch.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new EasyOsException("OpenSearch JDBC driver not found on classpath", e);
        }
        return DriverManager.getConnection(url, props);
    }

    public void probe() {
        try (Connection connection = openConnection()) {
            SqlProbe.verify(connection);
            probed = true;
        } catch (EasyOsException e) {
            throw e;
        } catch (Exception e) {
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
        if (properties.isPrintSql()) {
            log.info("SQL: {} | params={}", boundSql.getSql(), boundSql.getParams());
        }
        try (Connection connection = openConnection();
             PreparedStatement ps = connection.prepareStatement(boundSql.getSql())) {
            List<Object> params = boundSql.getParams();
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return ResultSetMapper.toMaps(rs);
            }
        } catch (Exception e) {
            throw translate(e);
        }
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
        if (msg.contains("sql") && (msg.contains("plugin") || msg.contains("404") || msg.contains("not found"))) {
            return EasyOsException.sqlPluginMissing();
        }
        if (e instanceof EasyOsException) {
            return (EasyOsException) e;
        }
        return new EasyOsException("OpenSearch JDBC execution failed: " + e.getMessage(), e);
    }
}
