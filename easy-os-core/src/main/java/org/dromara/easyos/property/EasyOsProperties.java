package org.dromara.easyos.property;

/**
 * easy-open-search 基础配置项（前缀：{@code easy-open-search}）。
 */
public class EasyOsProperties {
    /**
     * 是否启用 easy-open-search，默认开启。
     */
    private boolean enable = true;
    /**
     * 是否打印启动 Banner，默认开启。
     */
    private boolean banner = true;
    /**
     * 是否打印执行的 SQL（含参数、耗时、行数），默认开启。
     */
    private boolean printSql = true;
    /**
     * 连接协议：http / https，默认 http。
     */
    private String schema = "http";
    /**
     * OpenSearch 地址，格式 host:port，默认 127.0.0.1:9200。
     */
    private String address = "127.0.0.1:9200";
    /**
     * 用户名，可缺省。
     */
    private String username;
    /**
     * 密码，可缺省。
     */
    private String password;
    /**
     * 是否信任自签名 HTTPS 证书（仅建议开发环境），默认 true。
     */
    private boolean trustSelfSigned = true;
    /**
     * 全局配置（索引托管、主键策略、字段映射等）。
     */
    private GlobalConfig globalConfig = new GlobalConfig();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isBanner() {
        return banner;
    }

    public void setBanner(boolean banner) {
        this.banner = banner;
    }

    public boolean isPrintSql() {
        return printSql;
    }

    public void setPrintSql(boolean printSql) {
        this.printSql = printSql;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isTrustSelfSigned() {
        return trustSelfSigned;
    }

    public void setTrustSelfSigned(boolean trustSelfSigned) {
        this.trustSelfSigned = trustSelfSigned;
    }

    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
    }
}
