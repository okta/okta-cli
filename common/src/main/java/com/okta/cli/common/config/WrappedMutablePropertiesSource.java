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

import com.google.common.base.CaseFormat;
import com.okta.commons.lang.Strings;
import com.okta.sdk.impl.config.OptionalPropertiesSource;
import com.okta.sdk.impl.config.PropertiesSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

abstract class WrappedMutablePropertiesSource implements MutablePropertySource {

    private final PropertiesSource wrappedPropertySource;

    WrappedMutablePropertiesSource(PropertiesSource wrappedPropertySource) {
        this.wrappedPropertySource = new OptionalPropertiesSource(wrappedPropertySource);
    }

    @Override
    public Map<String, String> getProperties() {
        return wrappedPropertySource.getProperties();
    }

    @Override
    public String getProperty(String key) {

        // try the original key, then the camelKey version
        String result = getProperties().get(key);
        if (Strings.isEmpty(result)) {
            String camelKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key); // camel to kebab
            result = getProperties().get(camelKey);
        }
        return result;
    }

    Writer fileWriter(File file) throws FileNotFoundException {
        return new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
    }
}