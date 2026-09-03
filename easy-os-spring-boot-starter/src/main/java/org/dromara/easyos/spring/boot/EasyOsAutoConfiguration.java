package org.dromara.easyos.spring.boot;

import org.dromara.easyos.client.OpenSearchClientFactory;
import org.dromara.easyos.enums.ProcessIndexMode;
import org.dromara.easyos.index.IndexProcessor;
import org.dromara.easyos.index.ManualIndexProcessor;
import org.dromara.easyos.index.NotSmoothlyIndexProcessor;
import org.dromara.easyos.jdbc.JdbcExecutor;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.write.DocumentWriter;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "easy-open-search", name = "enable", havingValue = "true", matchIfMissing = true)
public class EasyOsAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "easy-open-search")
    @ConditionalOnMissingBean
    public EasyOsProperties easyOsProperties() {
        return new EasyOsProperties();
    }

    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean
    public OpenSearchClient openSearchClient(EasyOsProperties properties) {
        return OpenSearchClientFactory.create(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcExecutor jdbcExecutor(EasyOsProperties properties) {
        return new JdbcExecutor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentWriter documentWriter(OpenSearchClient client, EasyOsProperties properties) {
        return new DocumentWriter(client, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IndexProcessor indexProcessor(OpenSearchClient client, EasyOsProperties properties) {
        ProcessIndexMode mode = properties.getGlobalConfig().getProcessIndexMode();
        if (mode == ProcessIndexMode.NOT_SMOOTHLY) {
            return new NotSmoothlyIndexProcessor(client, properties);
        }
        return new ManualIndexProcessor(client, properties);
    }
}
