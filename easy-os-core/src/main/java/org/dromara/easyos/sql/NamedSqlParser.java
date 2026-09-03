package org.dromara.easyos.sql;

import org.dromara.easyos.exception.EasyOsException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 {@code #{name}} 转为 JDBC {@code ?}，并收集占位符名。不支持 {@code ${}}。
 */
public final class NamedSqlParser {
    private static final Pattern NAMED = Pattern.compile("#\\{\\s*([\\w.]+)\\s*}");
    private static final Pattern DOLLAR = Pattern.compile("\\$\\{");

    private NamedSqlParser() {
    }

    public static ParsedSql parse(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new EasyOsException("@OsSelect SQL must not be blank");
        }
        if (DOLLAR.matcher(sql).find()) {
            throw new EasyOsException("@OsSelect does not support ${} substitution; use #{} only");
        }
        Matcher matcher = NAMED.matcher(sql);
        StringBuffer sb = new StringBuffer();
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
            matcher.appendReplacement(sb, "?");
        }
        matcher.appendTail(sb);
        return new ParsedSql(sb.toString(), names);
    }

    public static final class ParsedSql {
        private final String jdbcSql;
        private final List<String> paramNames;

        public ParsedSql(String jdbcSql, List<String> paramNames) {
            this.jdbcSql = jdbcSql;
            this.paramNames = paramNames;
        }

        public String getJdbcSql() {
            return jdbcSql;
        }

        public List<String> getParamNames() {
            return paramNames;
        }
    }
}
