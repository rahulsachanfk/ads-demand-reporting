package com.flipkart.ad.dp;


import com.flipkart.cp.cfgsvc.ConfigServiceClientWrapper;

import java.util.Arrays;
import java.util.Map;

/**
 * ConfigProvider uses ConfigServiceClientWrapper to integrate with new config service
 * and provides dynamic config without app restart
 */
public class ConfigProvider {
    private final static ConfigProvider INSTANCE = new ConfigProvider();
    private ConfigServiceClientWrapper clientWrapper;

    public ConfigProvider() {
        clientWrapper = new ConfigServiceClientWrapper(Arrays.asList(Constant.CONFIG_SERVICE_BUCKET_NAME));
    }

    public static ConfigProvider getInstance() {
        return INSTANCE;
    }

    public String get(String configKey, String defaultValue) {
        String configValue = clientWrapper.get(configKey);
        if (configValue != null) {
            return configValue;
        }
        return defaultValue;
    }

    public Boolean getBoolean(String configKey, Boolean defaultValue) {
        Boolean configValue = clientWrapper.getBoolean(configKey);
        if (configValue != null) {
            return configValue;
        }
        return defaultValue;
    }

    public Integer getInt(String configKey, Integer defaultValue) {
        Integer configValue = clientWrapper.getInt(configKey);
        if (configValue != null) {
            return configValue;
        }
        return defaultValue;
    }

    public Double getDouble(String configKey, Double defaultValue) {
        Double configValue = clientWrapper.getDouble(configKey);
        if (configValue != null) {
            return configValue;
        }
        return defaultValue;
    }

    public Map<String, Object> getKeys(String bucketName) {
        return clientWrapper.getAll(bucketName);
    }
}
