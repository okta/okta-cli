package com.okta.cli.common.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapPropertySource implements MutablePropertySource {

    Map<String, String> properties = new LinkedHashMap<>();

    @Override
    public String getName() {
        return "console";
    }

    @Override
    public void addProperties(Map<String, String> properties) throws IOException {
        properties.forEach((key, value) -> {

            if (value == null) {
                this.properties.remove(key);
            } else {
                this.properties.put(key, value);
            }
        });
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }
}
