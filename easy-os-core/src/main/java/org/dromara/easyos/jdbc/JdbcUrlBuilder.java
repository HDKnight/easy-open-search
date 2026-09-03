package org.dromara.easyos.jdbc;

import org.dromara.easyos.property.EasyOsProperties;

public final class JdbcUrlBuilder {
    private JdbcUrlBuilder() {
    }

    public static String build(EasyOsProperties properties) {
        String schema = properties.getSchema() == null ? "http" : properties.getSchema();
        String address = properties.getAddress();
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("easy-open-search.address must not be blank");
        }
        // jdbc:opensearch://https://host:port  or jdbc:opensearch://host:port
        if ("https".equalsIgnoreCase(schema)) {
            return "jdbc:opensearch://https://" + address;
        }
        return "jdbc:opensearch://" + address;
    }
}
