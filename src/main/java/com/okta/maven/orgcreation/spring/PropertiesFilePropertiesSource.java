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
package com.okta.maven.orgcreation.spring;

import com.okta.sdk.impl.config.ResourcePropertiesSource;
import com.okta.sdk.impl.io.FileResource;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

public class PropertiesFilePropertiesSource extends WrappedMutablePropertiesSource {

    private final File propertiesFile;

    public PropertiesFilePropertiesSource(File propertiesFile) {
        super(new ResourcePropertiesSource(new FileResource(propertiesFile.getAbsolutePath())));
        this.propertiesFile = propertiesFile;
    }

    @Override
    public void addProperties(Map<String, String> properties) throws IOException {

        Properties existingProps = new Properties();
        existingProps.putAll(getProperties());
        existingProps.putAll(properties);

        try (Writer writer = fileWriter(propertiesFile)) {
            existingProps.store(writer, null);
        }
    }
}