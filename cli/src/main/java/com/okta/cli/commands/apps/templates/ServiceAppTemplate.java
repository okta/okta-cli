/*
 * Copyright 2020-Present Okta, Inc.
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
package com.okta.cli.commands.apps.templates;

import com.okta.cli.console.PromptOption;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ServiceAppTemplate implements PromptOption<ServiceAppTemplate> {

    OKTA_SPRING_BOOT("Okta Spring Boot Starter", null,  "src/main/resources/application.properties"),
    SPRING_BOOT("Spring Boot", "okta",  "src/main/resources/application.properties"),
    JHIPSTER("JHipster", "oidc", ".okta.env"),
    GENERIC("Other", null, ".okta.env");

    private static final Map<String, ServiceAppTemplate> nameToTemplateMap = Arrays.stream(values()).collect(Collectors.toMap(it -> it.friendlyName, it -> it));

    private final String friendlyName;
    private final String springPropertyKey;
    private final String defaultConfigFileName;

    ServiceAppTemplate(String friendlyName, String springPropertyKey, String defaultConfigFileName) {
        this.friendlyName = friendlyName;
        this.springPropertyKey = springPropertyKey;
        this.defaultConfigFileName = defaultConfigFileName;
    }

    public String getSpringPropertyKey() {
        return springPropertyKey;
    }

    public String getDefaultConfigFileName() {
        return defaultConfigFileName;
    }

    @Override
    public String displayName() {
        return friendlyName;
    }

    @Override
    public ServiceAppTemplate value() {
        return this;
    }
}
