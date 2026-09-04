package org.dromara.easyos.spring.boot;

import org.dromara.easyos.mapper.BaseMapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.Arrays;
import java.util.Set;

/**
 * Scans BaseMapper interfaces and registers them as {@link MapperFactoryBean}.
 */
public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner {

    public ClassPathMapperScanner(BeanDefinitionRegistry registry, Environment environment) {
        super(registry, false, environment);
    }

    public void registerFilters() {
        addIncludeFilter(new AssignableTypeFilter(BaseMapper.class) {
            @Override
            protected boolean matchClassName(String className) {
                // AssignableTypeFilter matches subclasses; exclude BaseMapper itself via candidate check
                return true;
            }
        });
        addExcludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            return className.equals(BaseMapper.class.getName()) || className.endsWith("package-info");
        });
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            logger.warn("[easy-os] 未扫描到 Mapper，请检查 @MapperScan 包路径: " + Arrays.toString(basePackages));
        } else {
            processBeanDefinitions(beanDefinitions);
            logger.info("[easy-os] Mapper 扫描完成: packages=" + Arrays.toString(basePackages)
                    + ", count=" + beanDefinitions.size());
        }
        return beanDefinitions;
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        for (BeanDefinitionHolder holder : beanDefinitions) {
            GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();
            String mapperInterface = definition.getBeanClassName();
            definition.getConstructorArgumentValues().clear();
            definition.getConstructorArgumentValues().addGenericArgumentValue(mapperInterface);
            // 让容器/工具链知道 FactoryBean 产出的真实类型（对齐 MyBatis）
            definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, mapperInterface);
            definition.setBeanClass(MapperFactoryBean.class);
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            definition.setPrimary(true);
            logger.info("[easy-os] 注册 MapperFactoryBean: " + mapperInterface);
        }
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface()
                && beanDefinition.getMetadata().isIndependent()
                && !beanDefinition.getMetadata().isAnnotation();
    }

    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        return super.checkCandidate(beanName, beanDefinition);
    }
}
