/*
 * Copyright 2020-Present Okta, Inc, Inc.
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

import com.google.common.base.CaseFormat;
import com.okta.commons.lang.Assert;
import com.okta.commons.lang.Strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A property source for env files in the format of:
 * <pre><code>
 * export KEY=value
 * </code></pre>
 * @since 0.3.0
 */
public class EnvFilePropertiesSource implements MutablePropertySource {

    private final File envFile;

    public EnvFilePropertiesSource(File envFile) {
        this.envFile = envFile;
    }

    @Override
    public void addProperties(Map<String, String> properties) throws IOException {

        Map<String, String> allProperties = new LinkedHashMap<>(getProperties()); // start with existing properties
        properties.forEach((key, value) -> {
            String upperKey = dottedCamelToUpperUnderscore(key);
            allProperties.put(upperKey, value);
        });

        try(Writer writer = new OutputStreamWriter(new FileOutputStream(envFile), UTF_8)) {
            for(Map.Entry<String, String> entry : allProperties.entrySet()) {
                writer.write("export "+ entry.getKey() + "=\"" + entry.getValue() + "\"\n");
            }
        }
    }

    @Override
    public String getProperty(String key) {
        String upperKey = dottedCamelToUpperUnderscore(key);
        return getProperties().get(upperKey);
    }

    @Override
    public Map<String, String> getProperties() {

        Map<String, String> result = new LinkedHashMap<>();
        if (envFile.exists()) {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(envFile), UTF_8))) {
                reader.lines()
                        .map(Strings::clean)
                        .filter(line -> !line.startsWith("#"))
                        .map(line -> line.replaceFirst("^export ", ""))
                        .forEach(line -> {
                            // the line is now clean and should be in the format of KEY=value, or KEY="value"
                            int splitAt = line.indexOf('=');
                            if (splitAt < 1) { // does not contain or starts with '='
                                throw new RuntimeException("Failed to parse key value pair '" + line +"' in file: "+ envFile);
                            }
                            String key = Strings.clean(line.substring(0, splitAt));
                            String value = unquote(Strings.clean(line.substring(splitAt + 1)));

                            result.put(key, value);
                        });

            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: "+ envFile.getAbsolutePath(), e);
            }
        }

        return result;
    }

    private String unquote(String value) {
        if (value.startsWith("\"")) {
            Assert.isTrue(value.endsWith("\""), "Invalid formatted value '" + value +"', started with a quote but did not end with one. NOTE: End of line comments are NOT supported");
            return value.substring(1, value.length() - 1); // strip start and end quotes
        }
        return value;
    }

    private String dottedCamelToUpperUnderscore(String value) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, value)
                .replaceAll("[-\\.]", "_");
    }
}
