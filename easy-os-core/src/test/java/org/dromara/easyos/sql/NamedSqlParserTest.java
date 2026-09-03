package org.dromara.easyos.sql;

import org.dromara.easyos.exception.EasyOsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamedSqlParserTest {

    @Test
    void replacesNamedParams() {
        NamedSqlParser.ParsedSql parsed = NamedSqlParser.parse(
                "SELECT * FROM article WHERE status = #{status} AND title = #{title}");
        assertEquals("SELECT * FROM article WHERE status = ? AND title = ?", parsed.getJdbcSql());
        assertEquals(2, parsed.getParamNames().size());
        assertEquals("status", parsed.getParamNames().get(0));
        assertEquals("title", parsed.getParamNames().get(1));
    }

    @Test
    void rejectsDollarSubstitution() {
        EasyOsException ex = assertThrows(EasyOsException.class,
                () -> NamedSqlParser.parse("SELECT * FROM ${table}"));
        assertTrue(ex.getMessage().contains("${}"));
    }
}
