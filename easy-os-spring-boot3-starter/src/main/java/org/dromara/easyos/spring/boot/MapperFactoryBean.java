package org.dromara.easyos.spring.boot;

import org.dromara.easyos.annotation.OsSelect;
import org.dromara.easyos.index.IndexProcessor;
import org.dromara.easyos.jdbc.JdbcExecutor;
import org.dromara.easyos.mapper.BaseMapper;
import org.dromara.easyos.mapper.BaseMapperImpl;
import org.dromara.easyos.mapper.OsSelectInvoker;
import org.dromara.easyos.property.EasyOsProperties;
import org.dromara.easyos.toolkit.EntityMeta;
import org.dromara.easyos.write.DocumentWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

public class MapperFactoryBean<T> implements FactoryBean<T>, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(MapperFactoryBean.class);

    private Class<T> mapperInterface;
    private ApplicationContext applicationContext;

    public MapperFactoryBean() {
    }

    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T getObject() {
        if (mapperInterface == null) {
            throw new IllegalStateException("mapperInterface must not be null");
        }
        Class<?> entityClass = resolveEntityClass(mapperInterface);
        EasyOsProperties properties = applicationContext.getBean(EasyOsProperties.class);
        JdbcExecutor jdbcExecutor = applicationContext.getBean(JdbcExecutor.class);
        DocumentWriter documentWriter = applicationContext.getBean(DocumentWriter.class);
        IndexProcessor indexProcessor = applicationContext.getBean(IndexProcessor.class);
        EntityMeta meta = EntityMeta.of(entityClass, properties);
        log.info("[easy-os] 初始化 Mapper: mapper={}, entity={}, index={}, mode={}",
                mapperInterface.getSimpleName(),
                entityClass.getSimpleName(),
                meta.getIndexName(),
                properties.getGlobalConfig().getProcessIndexMode());
        BaseMapperImpl<?> impl = new BaseMapperImpl(entityClass, properties, jdbcExecutor, documentWriter, indexProcessor);
        OsSelectInvoker osSelectInvoker = new OsSelectInvoker(entityClass, properties, jdbcExecutor);
        if (properties.getGlobalConfig() != null) {
            indexProcessor.processOnStartup(entityClass);
        }
        log.info("[easy-os] Mapper 就绪: {}", mapperInterface.getName());

        InvocationHandler handler = (proxy, method, args) -> {
            if (Object.class.equals(method.getDeclaringClass())) {
                return invokeObjectMethod(proxy, method, args);
            }
            if (method.getAnnotation(OsSelect.class) != null) {
                return osSelectInvoker.invoke(method, args);
            }
            if (method.isDefault()) {
                throw new UnsupportedOperationException(
                        "Default methods are not supported on Easy-OS Mapper: " + method);
            }
            try {
                if (args == null) {
                    return method.invoke(impl);
                }
                return method.invoke(impl, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw new IllegalStateException(cause);
            }
        };
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                handler);
    }

    private static Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return "EasyOsMapper(" + proxy.getClass().getInterfaces()[0].getName() + ")";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(name)) {
            return proxy == args[0];
        }
        throw new UnsupportedOperationException("Object method not supported: " + method);
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
        throw new IllegalStateException("Cannot resolve entity type from " + mapperInterface.getName()
                + ". Mapper must extend BaseMapper<Entity>.");
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
