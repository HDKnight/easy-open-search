package org.dromara.easyos.property;

public class EasyOsProperties {
    private boolean enable = true;
    private boolean banner = true;
    private boolean printSql = true;
    private String schema = "http";
    private String address = "127.0.0.1:9200";
    private String username;
    private String password;
    /**
     * Trust self-signed HTTPS certificates (dev only). Default true for https convenience.
     */
    private boolean trustSelfSigned = true;
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
