package org.dromara.easyos.spring.boot;

import org.dromara.easyos.client.OpenSearchClientFactory;
import org.dromara.easyos.enums.ProcessIndexMode;
import org.dromara.easyos.index.IndexProcessor;
import org.dromara.easyos.index.ManualIndexProcessor;
import org.dromara.easyos.index.NotSmoothlyIndexProcessor;
import org.dromara.easyos.jdbc.JdbcExecutor;
import org.dromara.easyos.jdbc.JdbcUrlBuilder;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.write.DocumentWriter;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "easy-open-search", name = "enable", havingValue = "true", matchIfMissing = true)
public class EasyOsAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(EasyOsAutoConfiguration.class);

    @Bean
    @ConfigurationProperties(prefix = "easy-open-search")
    @ConditionalOnMissingBean
    public EasyOsProperties easyOsProperties() {
        return new EasyOsProperties();
    }

    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean
    public OpenSearchClient openSearchClient(EasyOsProperties properties) {
        log.info("[easy-os] 初始化 OpenSearchClient: schema={}, address={}, trustSelfSigned={}",
                properties.getSchema(), properties.getAddress(), properties.isTrustSelfSigned());
        return OpenSearchClientFactory.create(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcExecutor jdbcExecutor(EasyOsProperties properties) {
        log.info("[easy-os] 初始化 JdbcExecutor: jdbcUrl={}", JdbcUrlBuilder.build(properties));
        return new JdbcExecutor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentWriter documentWriter(OpenSearchClient client, EasyOsProperties properties) {
        log.info("[easy-os] 初始化 DocumentWriter");
        return new DocumentWriter(client, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IndexProcessor indexProcessor(OpenSearchClient client, EasyOsProperties properties) {
        ProcessIndexMode mode = properties.getGlobalConfig().getProcessIndexMode();
        log.info("[easy-os] 索引托管模式: {}", mode);
        if (mode == ProcessIndexMode.NOT_SMOOTHLY) {
            log.info("[easy-os] 使用 NotSmoothlyIndexProcessor（启动时自动创建缺失索引）");
            return new NotSmoothlyIndexProcessor(client, properties);
        }
        log.info("[easy-os] 使用 ManualIndexProcessor（启动不自动建索引，需调用 createIndex）");
        return new ManualIndexProcessor(client, properties);
    }
}
