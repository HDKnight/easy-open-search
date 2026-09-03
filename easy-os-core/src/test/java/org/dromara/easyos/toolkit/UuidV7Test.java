package org.dromara.easyos.toolkit;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidV7Test {

    @Test
    void generatesVersion7AndVariant() {
        UUID uuid = UuidV7.nextUuid();
        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    void isTimeOrderedAndUnique() {
        Set<String> seen = new HashSet<>();
        String prev = null;
        for (int i = 0; i < 200; i++) {
            String id = UuidV7.next();
            assertTrue(seen.add(id), "duplicate: " + id);
            if (prev != null) {
                assertTrue(id.compareTo(prev) >= 0, "not ordered: " + prev + " -> " + id);
            }
            prev = id;
        }
    }
}
