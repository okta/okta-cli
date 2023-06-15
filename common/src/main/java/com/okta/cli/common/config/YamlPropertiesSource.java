/*
 * Copyright 2018-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.cli.common.config;

import com.okta.sdk.impl.config.YAMLPropertiesSource;
import com.okta.sdk.impl.io.FileResource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class YamlPropertiesSource extends WrappedMutablePropertiesSource {

    private final File yamlFile;

    public YamlPropertiesSource(File yamlFile) {
        super(new YAMLPropertiesSource(new FileResource(yamlFile.getAbsolutePath())));
        this.yamlFile = yamlFile;
    }

    @Override
    public String getName() {
        return yamlFile.getAbsolutePath();
    }

    @Override
    public void addProperties(Map<String, String> properties) throws IOException {

        Yaml springAppYaml = new Yaml(new Constructor(Map.class, new LoaderOptions()), new Representer(yamlOptions()));
        Map<String, Object> existingProperties = new HashMap<>();
        if (yamlFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(yamlFile), StandardCharsets.UTF_8)) {
                Map<? extends String,?> loadedProperties = springAppYaml.load(reader);

                // null if the file is empty
                if (loadedProperties != null) {
                    existingProperties.putAll(loadedProperties);
                }
            }
        }

        // Break up each property key and traverse the tree (add nodes were needed)
        properties.forEach((key, value) -> {

            // split the key on '.'
            String[] keyParts = key.split("\\.");

            Map<String, Object> currentNode = existingProperties;
            for (int ii = 0; ii < keyParts.length; ii++) {

                String nodeKey = keyParts[ii];

                // for the last node, just set the value
                if (ii == keyParts.length-1) {
                    currentNode.put(nodeKey, value);
                } else {
                    Map<String, Object> nextNode = (Map<String, Object>) currentNode.getOrDefault(nodeKey, new HashMap<String, Object>());
                    currentNode.put(nodeKey, nextNode);
                    currentNode = nextNode;
                }
            }
        });

        // now write the file
        File parentDir = yamlFile.getParentFile();
        if (!(parentDir.exists() || parentDir.mkdirs())) {
            throw new IOException("Unable to create directory: "+ parentDir.getAbsolutePath());
        }
        try (Writer writer = fileWriter(yamlFile)) {
            springAppYaml.dump(existingProperties, writer);
        }
    }

    private static DumperOptions yamlOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return options;
    }
}
