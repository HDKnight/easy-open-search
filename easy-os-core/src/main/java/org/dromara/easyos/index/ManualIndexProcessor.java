package org.dromara.easyos.index;

import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.toolkit.EntityMeta;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ManualIndexProcessor implements IndexProcessor {
    private static final Logger log = LoggerFactory.getLogger(ManualIndexProcessor.class);

    private final OpenSearchClient client;
    private final EasyOsProperties properties;
    private final IndexSchemaBuilder schemaBuilder = new IndexSchemaBuilder();

    public ManualIndexProcessor(OpenSearchClient client, EasyOsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void processOnStartup(Class<?> entityClass) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        log.info("[easy-os] process-index-mode=manual，跳过自动建索引: name={}, entity={}（可调用 mapper.createIndex()）",
                meta.getIndexName(), entityClass.getSimpleName());
    }

    @Override
    public boolean createIndex(Class<?> entityClass) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        String indexName = meta.getIndexName();
        log.info("[easy-os] 手动创建索引: name={}, entity={}", indexName, entityClass.getSimpleName());
        try {
            client.indices().create(schemaBuilder.buildCreateRequest(entityClass, properties));
            log.info("[easy-os] 手动创建索引成功: name={}", indexName);
            return true;
        } catch (IOException e) {
            log.error("[easy-os] 手动创建索引失败: name={}", indexName, e);
            throw new EasyOsException("createIndex failed", e);
        }
    }

    @Override
    public boolean deleteIndex(Class<?> entityClass) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        String indexName = meta.getIndexName();
        log.warn("[easy-os] 删除索引: name={}, entity={}", indexName, entityClass.getSimpleName());
        try {
            client.indices().delete(d -> d.index(indexName));
            log.info("[easy-os] 删除索引成功: name={}", indexName);
            return true;
        } catch (IOException e) {
            log.error("[easy-os] 删除索引失败: name={}", indexName, e);
            throw new EasyOsException("deleteIndex failed", e);
        }
    }

    @Override
    public boolean existsIndex(Class<?> entityClass) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        try {
            boolean exists = schemaBuilder.exists(client, meta.getIndexName());
            log.debug("[easy-os] 检查索引是否存在: name={}, exists={}", meta.getIndexName(), exists);
            return exists;
        } catch (IOException e) {
            log.error("[easy-os] 检查索引失败: name={}", meta.getIndexName(), e);
            throw new EasyOsException("existsIndex failed", e);
        }
    }
}
