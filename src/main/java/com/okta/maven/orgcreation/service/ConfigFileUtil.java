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
package com.okta.maven.orgcreation.service;

import com.okta.maven.orgcreation.spring.EnvFilePropertiesSource;
import com.okta.maven.orgcreation.spring.MutablePropertySource;
import com.okta.maven.orgcreation.spring.PropertiesFilePropertiesSource;
import com.okta.maven.orgcreation.spring.YamlPropertiesSource;

import java.io.File;

public class ConfigFileUtil {

    public static MutablePropertySource findSpringApplicationConfig(File projectRoot, File configFile) {

        // discover config file type
        if (configFile == null) {
             // yaml
            File yamlFile = new File(projectRoot, "src/main/resources/application.yml");
            File propsFile = new File(projectRoot, "src/main/resources/application.properties");

            if (propsFile.exists()) {
                return new PropertiesFilePropertiesSource(propsFile);
            }

            return new YamlPropertiesSource(yamlFile);
        } else {
            if (configFile.toString().endsWith(".yml")) {
                return new YamlPropertiesSource(configFile);
            } else if (configFile.toString().endsWith(".properties")) {
                return new PropertiesFilePropertiesSource(configFile);
            } else if (configFile.toString().endsWith(".env")) {
                return new EnvFilePropertiesSource(configFile);
            } else {
                throw new IllegalArgumentException("Unsupported config file type: " + configFile);
            }
        }
    }

}
