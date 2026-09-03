package org.dromara.easyos.annotation.rely;

/**
 * 主键生成策略。
 * <p>{@link #UUID} 使用时间有序的 UUIDv7，比随机 UUIDv4 更利于索引写入。
 */
public enum IdType {
    /**
     * 未在注解上显式指定时回退到全局配置 {@code easy-open-search.global-config.db-config.id-type}；
     * 生效为 NONE 时由 OpenSearch 自动生成 _id。
     */
    NONE,
    /**
     * 插入前由框架生成 UUIDv7，并写回实体（及可选写入 _source）。
     */
    UUID,
    /**
     * 由调用方自行传入 id，缺失则报错。
     */
    CUSTOMIZE
}
