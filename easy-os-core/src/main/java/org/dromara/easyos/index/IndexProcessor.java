package org.dromara.easyos.index;

public interface IndexProcessor {
    void processOnStartup(Class<?> entityClass);

    boolean createIndex(Class<?> entityClass);

    boolean deleteIndex(Class<?> entityClass);

    boolean existsIndex(Class<?> entityClass);
}
