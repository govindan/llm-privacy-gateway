package com.company.privacygateway.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String jwtSecret = "";
    private String vaultEncryptionKey = "";
    private String vaultHashKey = "";
    private String privacyDetectorUrl = "http://localhost:8000";
    private String privacyDetectorToken = "";
    private int tokenTtlHours = 24;
    private boolean devTokenEnabled = false;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getVaultEncryptionKey() {
        return vaultEncryptionKey;
    }

    public void setVaultEncryptionKey(String vaultEncryptionKey) {
        this.vaultEncryptionKey = vaultEncryptionKey;
    }

    public String getVaultHashKey() {
        return vaultHashKey;
    }

    public void setVaultHashKey(String vaultHashKey) {
        this.vaultHashKey = vaultHashKey;
    }

    public String getPrivacyDetectorUrl() {
        return privacyDetectorUrl;
    }

    public void setPrivacyDetectorUrl(String privacyDetectorUrl) {
        this.privacyDetectorUrl = privacyDetectorUrl;
    }

    public String getPrivacyDetectorToken() {
        return privacyDetectorToken;
    }

    public void setPrivacyDetectorToken(String privacyDetectorToken) {
        this.privacyDetectorToken = privacyDetectorToken;
    }

    public int getTokenTtlHours() {
        return tokenTtlHours;
    }

    public void setTokenTtlHours(int tokenTtlHours) {
        this.tokenTtlHours = tokenTtlHours;
    }

    public boolean isDevTokenEnabled() {
        return devTokenEnabled;
    }

    public void setDevTokenEnabled(boolean devTokenEnabled) {
        this.devTokenEnabled = devTokenEnabled;
    }
}
