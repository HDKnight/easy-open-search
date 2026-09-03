package org.dromara.easyos.toolkit;

import org.dromara.easyos.annotation.IndexField;
import org.dromara.easyos.annotation.IndexId;
import org.dromara.easyos.annotation.IndexName;
import org.dromara.easyos.annotation.Score;
import org.dromara.easyos.annotation.rely.IdType;
import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityMeta {
    private final Class<?> entityClass;
    private final String indexName;
    private final Field idField;
    private final IdType idType;
    private final boolean writeIdToSource;
    private final String idColumn;

    private EntityMeta(Class<?> entityClass, String indexName, Field idField,
                       IdType idType, boolean writeIdToSource, String idColumn) {
        this.entityClass = entityClass;
        this.indexName = indexName;
        this.idField = idField;
        this.idType = idType;
        this.writeIdToSource = writeIdToSource;
        this.idColumn = idColumn;
    }

    public static EntityMeta of(Class<?> entityClass, EasyOsProperties properties) {
        IndexName indexNameAnn = entityClass.getAnnotation(IndexName.class);
        if (indexNameAnn == null || indexNameAnn.value().trim().isEmpty()) {
            throw new EasyOsException("@IndexName is required on " + entityClass.getName());
        }
        String prefix = properties.getGlobalConfig().getDbConfig().getIndexPrefix();
        String indexName = (prefix == null ? "" : prefix) + indexNameAnn.value();
        Field idField = null;
        IndexId indexId = null;
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            IndexId ann = field.getAnnotation(IndexId.class);
            if (ann != null) {
                idField = field;
                indexId = ann;
                break;
            }
        }
        if (idField == null || indexId == null) {
            throw new EasyOsException("@IndexId is required on " + entityClass.getName());
        }

        IdType idType = indexId.type();
        if (idType == IdType.NONE) {
            idType = properties.getGlobalConfig().getDbConfig().getIdType();
            if (idType == null) {
                idType = IdType.NONE;
            }
        }

        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
        String idColumn = !indexId.value().isEmpty()
                ? indexId.value()
                : (mapUnderscore ? FieldUtils.camelToUnderline(idField.getName()) : idField.getName());

        return new EntityMeta(entityClass, indexName, idField, idType, indexId.writeToSource(), idColumn);
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getIndexName() {
        return indexName;
    }

    public Field getIdField() {
        return idField;
    }

    public IdType getIdType() {
        return idType;
    }

    public boolean isWriteIdToSource() {
        return writeIdToSource;
    }

    public Object readId(Object entity) {
        try {
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new EasyOsException("Cannot read id", e);
        }
    }

    public void writeId(Object entity, Object id) {
        try {
            Class<?> type = idField.getType();
            Object value = id;
            if (id != null && type == String.class && !(id instanceof String)) {
                value = String.valueOf(id);
            }
            idField.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new EasyOsException("Cannot write id", e);
        }
    }

    /**
     * 按策略准备插入用的文档 id：可能生成 UUIDv7、校验自定义 id，或返回 null（由 OpenSearch 生成）。
     */
    public String prepareInsertId(Object entity) {
        if (idType == IdType.UUID) {
            Object existing = readId(entity);
            if (existing != null && String.valueOf(existing).trim().length() > 0) {
                return String.valueOf(existing);
            }
            String uuid = UuidV7.next();
            writeId(entity, uuid);
            return uuid;
        }
        if (idType == IdType.CUSTOMIZE) {
            Object id = readId(entity);
            if (id == null || String.valueOf(id).trim().isEmpty()) {
                throw new EasyOsException("IdType.CUSTOMIZE requires non-null id on insert: " + entityClass.getName());
            }
            return String.valueOf(id);
        }
        // NONE: OpenSearch 自动生成；若调用方已传 id 则尊重
        Object id = readId(entity);
        if (id == null || String.valueOf(id).trim().isEmpty()) {
            return null;
        }
        return String.valueOf(id);
    }

    public Map<String, Object> toDocument(Object entity, boolean mapUnderscore) {
        Map<String, Object> doc = new LinkedHashMap<>();
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(Score.class) != null) {
                continue;
            }
            if (field.getAnnotation(IndexId.class) != null && !writeIdToSource) {
                continue;
            }
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }
                IndexField indexField = field.getAnnotation(IndexField.class);
                String column;
                if (field.getAnnotation(IndexId.class) != null) {
                    column = idColumn;
                } else if (indexField != null && !indexField.value().isEmpty()) {
                    column = indexField.value();
                } else {
                    column = mapUnderscore ? FieldUtils.camelToUnderline(field.getName()) : field.getName();
                }
                doc.put(column, value);
            } catch (IllegalAccessException e) {
                throw new EasyOsException("Cannot read field " + field.getName(), e);
            }
        }
        return doc;
    }
}
