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
        log.info("process-index-mode=manual, skip auto index for {}", entityClass.getSimpleName());
    }

    @Override
    public boolean createIndex(Class<?> entityClass) {
        try {
            client.indices().create(schemaBuilder.buildCreateRequest(entityClass, properties));
            return true;
        } catch (IOException e) {
            throw new EasyOsException("createIndex failed", e);
        }
    }

    @Override
    public boolean deleteIndex(Class<?> entityClass) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        try {
            client.indices().delete(d -> d.index(meta.getIndexName()));
            return true;
        } catch (IOException e) {
            throw new EasyOsException("deleteIndex failed", e);
        }
    }

    @Override
    public boolean existsIndex(Class<?> entityClass) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        try {
            return schemaBuilder.exists(client, meta.getIndexName());
        } catch (IOException e) {
            throw new EasyOsException("existsIndex failed", e);
        }
    }
}
