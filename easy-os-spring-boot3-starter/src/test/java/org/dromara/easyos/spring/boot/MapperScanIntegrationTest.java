package org.dromara.easyos.spring.boot;

import org.dromara.easyos.annotation.IndexId;
import org.dromara.easyos.annotation.IndexName;
import org.dromara.easyos.mapper.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MapperScanIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyOsAutoConfiguration.class))
            .withUserConfiguration(ScanConfig.class)
            .withPropertyValues(
                    "easy-open-search.enable=true",
                    "easy-open-search.address=127.0.0.1:9200",
                    "easy-open-search.global-config.process-index-mode=manual"
            );

    @Test
    void registersMapperBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DemoMapper.class);
            assertThat(context.getBean(DemoMapper.class)).isNotNull();
        });
    }

    @Configuration
    @MapperScan("org.dromara.easyos.spring.boot")
    static class ScanConfig {
    }

    @IndexName("demo")
    public static class DemoEntity {
        @IndexId
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public interface DemoMapper extends BaseMapper<DemoEntity> {
    }
}
