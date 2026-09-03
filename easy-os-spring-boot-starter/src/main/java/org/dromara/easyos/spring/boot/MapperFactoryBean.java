package org.dromara.easyos.spring.boot;

import org.dromara.easyos.index.IndexProcessor;
import org.dromara.easyos.jdbc.JdbcExecutor;
import org.dromara.easyos.mapper.BaseMapper;
import org.dromara.easyos.mapper.BaseMapperImpl;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.write.DocumentWriter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

public class MapperFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {
    private final Class<T> mapperInterface;
    private ApplicationContext applicationContext;

    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T getObject() {
        Class<?> entityClass = resolveEntityClass(mapperInterface);
        EasyOsProperties properties = applicationContext.getBean(EasyOsProperties.class);
        JdbcExecutor jdbcExecutor = applicationContext.getBean(JdbcExecutor.class);
        DocumentWriter documentWriter = applicationContext.getBean(DocumentWriter.class);
        IndexProcessor indexProcessor = applicationContext.getBean(IndexProcessor.class);
        BaseMapperImpl<?> impl = new BaseMapperImpl(entityClass, properties, jdbcExecutor, documentWriter, indexProcessor);
        indexProcessor.processOnStartup(entityClass);
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(impl, args);
                    }
                    return method.invoke(impl, args);
                });
    }

    private Class<?> resolveEntityClass(Class<?> mapperInterface) {
        for (Type type : mapperInterface.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType() == BaseMapper.class) {
                    return (Class<?>) pt.getActualTypeArguments()[0];
                }
            }
        }
        throw new IllegalStateException("Cannot resolve entity type from " + mapperInterface.getName());
    }

    @Override
    public Class<?> getObjectType() {
        return mapperInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
