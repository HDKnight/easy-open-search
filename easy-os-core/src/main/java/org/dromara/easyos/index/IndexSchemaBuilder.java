package org.dromara.easyos.index;

import org.dromara.easyos.annotation.IndexField;
import org.dromara.easyos.annotation.IndexId;
import org.dromara.easyos.annotation.Score;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.toolkit.EntityMeta;
import org.dromara.easyos.toolkit.FieldUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class IndexSchemaBuilder {

    public CreateIndexRequest buildCreateRequest(Class<?> entityClass, EasyOsProperties properties) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
        Map<String, Property> props = new HashMap<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getAnnotation(Score.class) != null) {
                continue;
            }
            IndexField indexField = field.getAnnotation(IndexField.class);
            String column = indexField != null && !indexField.value().isEmpty()
                    ? indexField.value()
                    : (mapUnderscore ? FieldUtils.camelToUnderline(field.getName()) : field.getName());
            String type = indexField != null && !indexField.type().isEmpty()
                    ? indexField.type()
                    : inferType(field);
            props.put(column, toProperty(type));
        }
        TypeMapping mapping = new TypeMapping.Builder().properties(props).build();
        return new CreateIndexRequest.Builder()
                .index(meta.getIndexName())
                .mappings(mapping)
                .build();
    }

    private String inferType(Field field) {
        Class<?> t = field.getType();
        if (t == String.class) {
            return field.getAnnotation(IndexId.class) != null ? "keyword" : "text";
        }
        if (t == Integer.class || t == int.class) {
            return "integer";
        }
        if (t == Long.class || t == long.class) {
            return "long";
        }
        if (t == Double.class || t == double.class || t == Float.class || t == float.class) {
            return "double";
        }
        if (t == Boolean.class || t == boolean.class) {
            return "boolean";
        }
        return "keyword";
    }

    private Property toProperty(String type) {
        switch (type) {
            case "text":
                return Property.of(p -> p.text(t -> t));
            case "keyword":
                return Property.of(p -> p.keyword(k -> k));
            case "integer":
                return Property.of(p -> p.integer(i -> i));
            case "long":
                return Property.of(p -> p.long_(l -> l));
            case "double":
                return Property.of(p -> p.double_(d -> d));
            case "boolean":
                return Property.of(p -> p.boolean_(b -> b));
            case "date":
                return Property.of(p -> p.date(d -> d));
            default:
                return Property.of(p -> p.keyword(k -> k));
        }
    }

    public boolean exists(OpenSearchClient client, String index) throws IOException {
        return client.indices().exists(ExistsRequest.of(e -> e.index(index))).value();
    }
}
