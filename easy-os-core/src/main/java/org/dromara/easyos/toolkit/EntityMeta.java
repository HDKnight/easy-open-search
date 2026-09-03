package org.dromara.easyos.toolkit;

import org.dromara.easyos.annotation.IndexField;
import org.dromara.easyos.annotation.IndexId;
import org.dromara.easyos.annotation.IndexName;
import org.dromara.easyos.annotation.Score;
import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityMeta {
    private final Class<?> entityClass;
    private final String indexName;
    private final Field idField;

    private EntityMeta(Class<?> entityClass, String indexName, Field idField) {
        this.entityClass = entityClass;
        this.indexName = indexName;
        this.idField = idField;
    }

    public static EntityMeta of(Class<?> entityClass, EasyOsProperties properties) {
        IndexName indexNameAnn = entityClass.getAnnotation(IndexName.class);
        if (indexNameAnn == null || indexNameAnn.value().trim().isEmpty()) {
            throw new EasyOsException("@IndexName is required on " + entityClass.getName());
        }
        String prefix = properties.getGlobalConfig().getDbConfig().getIndexPrefix();
        String indexName = (prefix == null ? "" : prefix) + indexNameAnn.value();
        Field idField = null;
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(IndexId.class) != null) {
                idField = field;
                break;
            }
        }
        if (idField == null) {
            throw new EasyOsException("@IndexId is required on " + entityClass.getName());
        }
        return new EntityMeta(entityClass, indexName, idField);
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

    public Object readId(Object entity) {
        try {
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new EasyOsException("Cannot read id", e);
        }
    }

    public Map<String, Object> toDocument(Object entity, boolean mapUnderscore) {
        Map<String, Object> doc = new LinkedHashMap<>();
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(Score.class) != null) {
                continue;
            }
            try {
                Object value = field.get(entity);
                if (value == null) {
                    continue;
                }
                IndexField indexField = field.getAnnotation(IndexField.class);
                String column = indexField != null && !indexField.value().isEmpty()
                        ? indexField.value()
                        : (mapUnderscore ? FieldUtils.camelToUnderline(field.getName()) : field.getName());
                doc.put(column, value);
            } catch (IllegalAccessException e) {
                throw new EasyOsException("Cannot read field " + field.getName(), e);
            }
        }
        return doc;
    }
}
