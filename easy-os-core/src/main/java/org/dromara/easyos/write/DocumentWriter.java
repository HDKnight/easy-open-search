package org.dromara.easyos.write;

import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.toolkit.EntityMeta;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.UpdateResponse;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class DocumentWriter {
    private final OpenSearchClient client;
    private final EasyOsProperties properties;

    public DocumentWriter(OpenSearchClient client, EasyOsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public <T> int insert(T entity) {
        EntityMeta meta = EntityMeta.of(entity.getClass(), properties);
        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
        Map<String, Object> doc = meta.toDocument(entity, mapUnderscore);
        Object id = meta.readId(entity);
        try {
            IndexResponse response = client.index(i -> {
                i.index(meta.getIndexName()).document(doc);
                if (id != null) {
                    i.id(String.valueOf(id));
                }
                return i;
            });
            return response.result() != null ? 1 : 0;
        } catch (IOException e) {
            throw new EasyOsException("insert failed", e);
        }
    }

    public <T> int insertBatch(Collection<T> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        T first = list.iterator().next();
        EntityMeta meta = EntityMeta.of(first.getClass(), properties);
        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (T entity : list) {
                Object id = meta.readId(entity);
                Map<String, Object> doc = meta.toDocument(entity, mapUnderscore);
                br.operations(op -> op.index(idx -> {
                    idx.index(meta.getIndexName()).document(doc);
                    if (id != null) {
                        idx.id(String.valueOf(id));
                    }
                    return idx;
                }));
            }
            BulkResponse response = client.bulk(br.build());
            return response.items() == null ? 0 : response.items().size();
        } catch (IOException e) {
            throw new EasyOsException("insertBatch failed", e);
        }
    }

    public <T> int updateById(T entity) {
        EntityMeta meta = EntityMeta.of(entity.getClass(), properties);
        Object id = meta.readId(entity);
        if (id == null) {
            throw new EasyOsException("updateById requires non-null id");
        }
        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
        Map<String, Object> doc = meta.toDocument(entity, mapUnderscore);
        try {
            UpdateResponse<?> response = client.update(u -> u
                    .index(meta.getIndexName())
                    .id(String.valueOf(id))
                    .doc(doc), Map.class);
            return response.result() != null ? 1 : 0;
        } catch (IOException e) {
            throw new EasyOsException("updateById failed", e);
        }
    }

    public int deleteById(Class<?> entityClass, Serializable id) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        try {
            DeleteResponse response = client.delete(d -> d.index(meta.getIndexName()).id(String.valueOf(id)));
            return response.result() != null ? 1 : 0;
        } catch (IOException e) {
            throw new EasyOsException("deleteById failed", e);
        }
    }
}
