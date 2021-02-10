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

import com.okta.cli.common.model.OidcProperties;
import com.okta.cli.console.PromptOption;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;

import java.util.List;

public class ServiceAppTemplate implements PromptOption<ServiceAppTemplate> {

    public static final ServiceAppTemplate OKTA_SPRING_BOOT = new ServiceAppTemplate("Okta Spring Boot Starter", OidcProperties.oktaEnv(),  "src/main/resources/application.properties");

    public static final ServiceAppTemplate SPRING_BOOT = new ServiceAppTemplate("Spring Boot", OidcProperties.spring("okta"),  "src/main/resources/application.properties");

    public static final ServiceAppTemplate JHIPSTER = jhipster();

    public static final ServiceAppTemplate QUARKUS = new ServiceAppTemplate("Quarkus", OidcProperties.quarkus(), "src/main/resources/application.properties");

    public static final ServiceAppTemplate GENERIC = new ServiceAppTemplate("Other", OidcProperties.oktaEnv(), ".okta.env");

    public static List<PromptOption<ServiceAppTemplate>> values() {
        return List.of(OKTA_SPRING_BOOT, SPRING_BOOT, JHIPSTER, QUARKUS, GENERIC);
    }

    private final String friendlyName;
    private final OidcProperties oidcProperties;
    private final String defaultConfigFileName;

    ServiceAppTemplate(String friendlyName, OidcProperties oidcProperties, String defaultConfigFileName) {
        this.friendlyName = friendlyName;
        this.oidcProperties = oidcProperties;
        this.defaultConfigFileName = defaultConfigFileName;
    }

    public OidcProperties getOidcProperties() {
        return oidcProperties;
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

    private static ServiceAppTemplate jhipster() {

        // defaults
        OidcProperties oidcProperties;
        String defaultConfigFile = ".okta.env";

        switch (JHipsterUtil.getGenerator()) {
            case QUARKUS: {
                oidcProperties = OidcProperties.quarkus(OpenIdConnectApplicationType.SERVICE);
                break;
            }
            case MICRONAUT: {
                oidcProperties = OidcProperties.micronaut(OpenIdConnectApplicationType.SERVICE);
                break;
            }
            default: {
                oidcProperties = OidcProperties.spring("oidc"); // default set of properties
            }
        }

        return new ServiceAppTemplate("JHipster", oidcProperties, defaultConfigFile);
    }
}
