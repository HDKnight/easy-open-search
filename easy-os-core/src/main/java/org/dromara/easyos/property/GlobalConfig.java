package org.dromara.easyos.property;

import org.dromara.easyos.annotation.rely.IdType;
import org.dromara.easyos.enums.ProcessIndexMode;

public class GlobalConfig {
    private ProcessIndexMode processIndexMode = ProcessIndexMode.MANUAL;
    private boolean asyncProcessIndexBlocking = true;
    private boolean distributed = true;
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

    public static class DbConfig {
        private String indexPrefix = "";
        private boolean mapUnderscoreToCamelCase = true;
        /** 全局主键策略；注解 type=NONE 时回退到此值。UUID 实际生成 UUIDv7。 */
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
