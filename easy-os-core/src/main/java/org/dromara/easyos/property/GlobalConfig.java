package org.dromara.easyos.property;

import org.dromara.easyos.annotation.rely.IdType;
import org.dromara.easyos.enums.ProcessIndexMode;

/**
 * 全局配置。
 */
public class GlobalConfig {
    /**
     * 索引处理模式：manual（手动）/ not_smoothly（启动自动创建缺失索引）。
     * smoothly（平滑迁移）预留，当前版本未实现。
     */
    private ProcessIndexMode processIndexMode = ProcessIndexMode.MANUAL;
    /**
     * 异步处理索引时是否阻塞主线程，默认阻塞。
     * （预留给后续平滑/异步索引场景。）
     */
    private boolean asyncProcessIndexBlocking = true;
    /**
     * 是否分布式环境，默认为是。
     * （预留给后续平滑模式多节点协调。）
     */
    private boolean distributed = true;
    /**
     * 数据 / 索引侧配置（前缀、驼峰、主键策略等）。
     */
    private DbConfig dbConfig = new DbConfig();

    public ProcessIndexMode getProcessIndexMode() {
        return processIndexMode;
    }

    public void setProcessIndexMode(ProcessIndexMode processIndexMode) {
        this.processIndexMode = processIndexMode;
    }

    public boolean isAsyncProcessIndexBlocking() {
        return asyncProcessIndexBlocking;
    }

    public void setAsyncProcessIndexBlocking(boolean asyncProcessIndexBlocking) {
        this.asyncProcessIndexBlocking = asyncProcessIndexBlocking;
    }

    public boolean isDistributed() {
        return distributed;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }

    public DbConfig getDbConfig() {
        return dbConfig;
    }

    public void setDbConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * 库表 / 索引字段相关配置。
     */
    public static class DbConfig {
        /**
         * 索引名前缀，例如 daily_，可缺省。
         */
        private String indexPrefix = "";
        /**
         * 是否开启下划线与驼峰互转，默认开启。
         */
        private boolean mapUnderscoreToCamelCase = true;
        /**
         * 全局主键策略；实体 {@code @IndexId(type=NONE)} 时回退到此值。
         * none：由 OpenSearch 自动生成；uuid：框架生成 UUIDv7；customize：必须自行传入 id。
         */
        private IdType idType = IdType.NONE;

        public String getIndexPrefix() {
            return indexPrefix;
        }

        public void setIndexPrefix(String indexPrefix) {
            this.indexPrefix = indexPrefix;
        }

        public boolean isMapUnderscoreToCamelCase() {
            return mapUnderscoreToCamelCase;
        }

        public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
            this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
        }

        public IdType getIdType() {
            return idType;
        }

        public void setIdType(IdType idType) {
            this.idType = idType;
        }
    }
}
