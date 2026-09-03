package org.dromara.easyos.spring.boot;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String basePackage = (String) importingClassMetadata
                .getAnnotationAttributes(MapperScan.class.getName())
                .get("value");
        if (!StringUtils.hasText(basePackage)) {
            return;
        }
        for (Class<?> mapperClass : findMapperInterfaces(basePackage)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperFactoryBean.class);
            builder.addConstructorArgValue(mapperClass);
            registry.registerBeanDefinition(mapperClass.getName(), builder.getBeanDefinition());
        }
    }

    private Set<Class<?>> findMapperInterfaces(String basePackage) {
        Set<Class<?>> result = new HashSet<>();
        String path = basePackage.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (!"file".equals(url.getProtocol())) {
                    continue;
                }
                File dir = new File(url.getFile());
                scanDirectory(dir, basePackage, result);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to scan mappers in " + basePackage, e);
        }
        return result;
    }

    private void scanDirectory(File dir, String packageName, Set<Class<?>> result) throws ClassNotFoundException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), result);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                Class<?> clazz = ClassUtils.forName(className, Thread.currentThread().getContextClassLoader());
                if (clazz.isInterface() && org.dromara.easyos.mapper.BaseMapper.class.isAssignableFrom(clazz)
                        && clazz != org.dromara.easyos.mapper.BaseMapper.class) {
                    result.add(clazz);
                }
            }
        }
    }
}
