package org.dromara.easyos.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyOsExceptionTest {

    @Test
    void sqlPluginMissingMessageContainsInstallHint() {
        EasyOsException ex = EasyOsException.sqlPluginMissing();
        assertTrue(ex.getMessage().contains("opensearch-sql"));
        assertTrue(ex.getMessage().contains("opensearch-plugin install"));
    }
}
