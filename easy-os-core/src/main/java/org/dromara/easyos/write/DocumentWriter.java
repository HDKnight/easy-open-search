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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class DocumentWriter {
    private static final Logger log = LoggerFactory.getLogger(DocumentWriter.class);

    private final OpenSearchClient client;
    private final EasyOsProperties properties;

    public DocumentWriter(OpenSearchClient client, EasyOsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public <T> int insert(T entity) {
        EntityMeta meta = EntityMeta.of(entity.getClass(), properties);
        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
        String id = meta.prepareInsertId(entity);
        Map<String, Object> doc = meta.toDocument(entity, mapUnderscore);
        log.info("[easy-os] insert: index={}, id={}, idType={}", meta.getIndexName(), id, meta.getIdType());
        try {
            IndexResponse response = client.index(i -> {
                i.index(meta.getIndexName()).document(doc);
                if (id != null) {
                    i.id(id);
                }
                return i;
            });
            if (id == null && response.id() != null) {
                meta.writeId(entity, response.id());
            }
            log.debug("[easy-os] insert result: index={}, id={}, result={}",
                    meta.getIndexName(), response.id(), response.result());
            return response.result() != null ? 1 : 0;
        } catch (IOException e) {
            log.error("[easy-os] insert failed: index={}, id={}", meta.getIndexName(), id, e);
            throw new EasyOsException("insert failed", e);
        }
    }

    public <T> int insertBatch(Collection<T> list) {
        if (list == null || list.isEmpty()) {
            log.info("[easy-os] insertBatch skipped: empty collection");
            return 0;
        }
        T first = list.iterator().next();
        EntityMeta meta = EntityMeta.of(first.getClass(), properties);
        boolean mapUnderscore = properties.getGlobalConfig().getDbConfig().isMapUnderscoreToCamelCase();
        log.info("[easy-os] insertBatch: index={}, size={}, idType={}",
                meta.getIndexName(), list.size(), meta.getIdType());
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (T entity : list) {
                String id = meta.prepareInsertId(entity);
                Map<String, Object> doc = meta.toDocument(entity, mapUnderscore);
                br.operations(op -> op.index(idx -> {
                    idx.index(meta.getIndexName()).document(doc);
                    if (id != null) {
                        idx.id(id);
                    }
                    return idx;
                }));
            }
            BulkResponse response = client.bulk(br.build());
            int size = response.items() == null ? 0 : response.items().size();
            if (response.errors()) {
                log.warn("[easy-os] insertBatch finished with errors: index={}, items={}", meta.getIndexName(), size);
            } else {
                log.info("[easy-os] insertBatch success: index={}, items={}", meta.getIndexName(), size);
            }
            return size;
        } catch (IOException e) {
            log.error("[easy-os] insertBatch failed: index={}", meta.getIndexName(), e);
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
        log.info("[easy-os] updateById: index={}, id={}", meta.getIndexName(), id);
        try {
            UpdateResponse<?> response = client.update(u -> u
                    .index(meta.getIndexName())
                    .id(String.valueOf(id))
                    .doc(doc), Map.class);
            log.debug("[easy-os] updateById result: index={}, id={}, result={}",
                    meta.getIndexName(), id, response.result());
            return response.result() != null ? 1 : 0;
        } catch (IOException e) {
            log.error("[easy-os] updateById failed: index={}, id={}", meta.getIndexName(), id, e);
            throw new EasyOsException("updateById failed", e);
        }
    }

    public int deleteById(Class<?> entityClass, Serializable id) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        log.info("[easy-os] deleteById: index={}, id={}", meta.getIndexName(), id);
        try {
            DeleteResponse response = client.delete(d -> d.index(meta.getIndexName()).id(String.valueOf(id)));
            log.debug("[easy-os] deleteById result: index={}, id={}, result={}",
                    meta.getIndexName(), id, response.result());
            return response.result() != null ? 1 : 0;
        } catch (IOException e) {
            log.error("[easy-os] deleteById failed: index={}, id={}", meta.getIndexName(), id, e);
            throw new EasyOsException("deleteById failed", e);
        }
    }
}
