package org.dromara.easyos.spring.boot;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes
                .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));
        if (attributes == null) {
            return;
        }
        List<String> basePackages = new ArrayList<>();
        basePackages.addAll(Arrays.stream(attributes.getStringArray("value"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toList()));
        basePackages.addAll(Arrays.stream(attributes.getStringArray("basePackages"))
                .filter(StringUtils::hasText)
                .collect(Collectors.toList()));
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry, environment);
        scanner.registerFilters();
        scanner.doScan(StringUtils.toStringArray(basePackages));
    }
}
