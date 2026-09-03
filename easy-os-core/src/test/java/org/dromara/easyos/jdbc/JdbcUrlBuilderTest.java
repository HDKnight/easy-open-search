package org.dromara.easyos.jdbc;

import org.dromara.easyos.property.EasyOsProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcUrlBuilderTest {

    @Test
    void buildJdbcUrlHttp() {
        EasyOsProperties p = new EasyOsProperties();
        p.setSchema("http");
        p.setAddress("127.0.0.1:9200");
        assertEquals("jdbc:opensearch://127.0.0.1:9200", JdbcUrlBuilder.build(p));
    }

    @Test
    void buildJdbcUrlHttps() {
        EasyOsProperties p = new EasyOsProperties();
        p.setSchema("https");
        p.setAddress("127.0.0.1:9200");
        assertEquals("jdbc:opensearch://https://127.0.0.1:9200", JdbcUrlBuilder.build(p));
    }
}
