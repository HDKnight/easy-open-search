package org.dromara.easyos.toolkit;

import org.dromara.easyos.annotation.IndexId;
import org.dromara.easyos.annotation.IndexName;
import org.dromara.easyos.annotation.rely.IdType;
import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.property.GlobalConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityMetaIdTypeTest {

    @Test
    void uuidStrategyGeneratesUuidV7() {
        EasyOsProperties properties = properties(IdType.NONE);
        EntityMeta meta = EntityMeta.of(UuidEntity.class, properties);
        UuidEntity entity = new UuidEntity();
        String id = meta.prepareInsertId(entity);
        assertNotNull(id);
        assertEquals(id, entity.id);
        assertEquals(7, java.util.UUID.fromString(id).version());
    }

    @Test
    void noneStrategyLeavesIdNull() {
        EasyOsProperties properties = properties(IdType.NONE);
        EntityMeta meta = EntityMeta.of(DefaultEntity.class, properties);
        DefaultEntity entity = new DefaultEntity();
        assertNull(meta.prepareInsertId(entity));
    }

    @Test
    void customizeRequiresId() {
        EasyOsProperties properties = properties(IdType.NONE);
        EntityMeta meta = EntityMeta.of(CustomizeEntity.class, properties);
        assertThrows(EasyOsException.class, () -> meta.prepareInsertId(new CustomizeEntity()));
    }

    @Test
    void annotationNoneFallsBackToGlobalUuid() {
        EasyOsProperties properties = properties(IdType.UUID);
        EntityMeta meta = EntityMeta.of(DefaultEntity.class, properties);
        assertEquals(IdType.UUID, meta.getIdType());
        DefaultEntity entity = new DefaultEntity();
        assertTrue(meta.prepareInsertId(entity).length() > 0);
    }

    private static EasyOsProperties properties(IdType idType) {
        EasyOsProperties properties = new EasyOsProperties();
        GlobalConfig.DbConfig dbConfig = properties.getGlobalConfig().getDbConfig();
        dbConfig.setIdType(idType);
        return properties;
    }

    @IndexName("uuid_demo")
    static class UuidEntity {
        @IndexId(type = IdType.UUID)
        private String id;
    }

    @IndexName("default_demo")
    static class DefaultEntity {
        @IndexId
        private String id;
    }

    @IndexName("customize_demo")
    static class CustomizeEntity {
        @IndexId(type = IdType.CUSTOMIZE)
        private String id;
    }
}
