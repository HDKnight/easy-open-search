package org.dromara.easyos.index;

import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.toolkit.EntityMeta;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Startup: create index when missing.
 * Explicit recreate still available via {@link #createIndex(Class)} after delete.
 */
public class NotSmoothlyIndexProcessor implements IndexProcessor {
    private static final Logger log = LoggerFactory.getLogger(NotSmoothlyIndexProcessor.class);

    private final OpenSearchClient client;
    private final EasyOsProperties properties;
    private final IndexSchemaBuilder schemaBuilder = new IndexSchemaBuilder();

    public NotSmoothlyIndexProcessor(OpenSearchClient client, EasyOsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void processOnStartup(Class<?> entityClass) {
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        try {
            boolean exists = schemaBuilder.exists(client, meta.getIndexName());
            if (exists) {
                log.info("Index {} already exists, skip auto-create (not_smoothly)", meta.getIndexName());
                return;
            }
            client.indices().create(schemaBuilder.buildCreateRequest(entityClass, properties));
            log.info("Index {} created automatically (not_smoothly)", meta.getIndexName());
        } catch (IOException e) {
            throw new EasyOsException("not_smoothly index process failed for " + meta.getIndexName(), e);
        }
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
