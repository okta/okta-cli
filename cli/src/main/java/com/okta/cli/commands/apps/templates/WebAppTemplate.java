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

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class WebAppTemplate implements PromptOption<WebAppTemplate> {


    public static final WebAppTemplate OKTA_SPRING_BOOT = new WebAppTemplate("Okta Spring Boot Starter", OidcProperties.oktaEnv(), "src/main/resources/application.properties", "http://localhost:8080/login/oauth2/code/okta", "/", null);

    public static final WebAppTemplate SPRING_BOOT = new WebAppTemplate("Spring Boot", OidcProperties.spring("okta"), "src/main/resources/application.properties", "http://localhost:8080/login/oauth2/code/okta", "/", null);

    public static final WebAppTemplate JHIPSTER = jhipster();

    public static final WebAppTemplate QUARKUS = new WebAppTemplate("Quarkus", OidcProperties.quarkus(OpenIdConnectApplicationType.WEB), "src/main/resources/application.properties", "http://localhost:8080/callback", "/", null);

    public static final WebAppTemplate GENERIC = new WebAppTemplate("Other", OidcProperties.oktaEnv(), ".okta.env", "http://localhost:8080/callback", "/", null);

    public static List<PromptOption<WebAppTemplate>> values() {
        return List.of(OKTA_SPRING_BOOT, SPRING_BOOT, JHIPSTER, QUARKUS, GENERIC);
    }

    private final String friendlyName;
    private final OidcProperties oidcProperties;
    private final String defaultConfigFileName;
    private final List<String> defaultRedirectUris;
    private final String defaultPostLogoutEndpoint;
    private final String groupsClaim;
    public final Set<String> groupsToCreate;

    WebAppTemplate(String friendlyName, OidcProperties oidcProperties, String defaultConfigFileName, List<String> defaultRedirectUris, String defaultPostLogoutEndpoint, String groupsClaim, Set<String> groupsToCreate) {
        this.friendlyName = friendlyName;
        this.oidcProperties = oidcProperties;
        this.defaultConfigFileName = defaultConfigFileName;
        this.defaultRedirectUris = Collections.unmodifiableList(defaultRedirectUris);
        this.defaultPostLogoutEndpoint = defaultPostLogoutEndpoint;
        this.groupsClaim = groupsClaim;
        this.groupsToCreate = Collections.unmodifiableSet(groupsToCreate);
    }

    WebAppTemplate(String friendlyName, OidcProperties oidcProperties, String defaultConfigFileName, String defaultRedirectUri, String defaultPostLogoutEndpoint, String groupsClaim) {
        this(friendlyName, oidcProperties, defaultConfigFileName, Collections.singletonList(defaultRedirectUri), defaultPostLogoutEndpoint, groupsClaim, Collections.emptySet());
    }

    public OidcProperties getOidcProperties() {
        return oidcProperties;
    }

    public String getDefaultConfigFileName() {
        return defaultConfigFileName;
    }

    public List<String> getDefaultRedirectUris() {
        return defaultRedirectUris;
    }

    public String getDefaultPostLogoutEndpoint() {
        return defaultPostLogoutEndpoint;
    }

    public String getGroupsClaim() {
        return groupsClaim;
    }

    public Set<String> getGroupsToCreate() {
        return groupsToCreate;
    }

    @Override
    public String displayName() {
        return friendlyName;
    }

    @Override
    public WebAppTemplate value() {
        return this;
    }

    private static WebAppTemplate jhipster() {

        // defaults
        OidcProperties oidcProperties;
        List<String> redirectUris = OKTA_SPRING_BOOT.defaultRedirectUris;
        String defaultPostLogoutEndpoint = OKTA_SPRING_BOOT.defaultPostLogoutEndpoint;
        String defaultConfigFile = ".okta.env";

        // jhipster is a generator, so the underlying project could be spring, quarkus, or something else
        // attempt to figure out the delegate but fallback to the default spring impl
        switch (JHipsterUtil.getGenerator()) {
            case QUARKUS: {
                oidcProperties = OidcProperties.quarkus(OpenIdConnectApplicationType.WEB);
                break;
            }
            case MICRONAUT: {
                oidcProperties = OidcProperties.micronaut(OpenIdConnectApplicationType.WEB);
                redirectUris = List.of("http://localhost:8080/oauth2/callback/oidc", "http://localhost:8761/oauth2/callback/oidc");
                defaultPostLogoutEndpoint = "/logout";
                break;
            }
            default: {
                oidcProperties = OidcProperties.spring("oidc");
            }
        }

        return new WebAppTemplate("JHipster",
                oidcProperties,
                defaultConfigFile,
                redirectUris,
                defaultPostLogoutEndpoint,
                "groups",
                Set.of("ROLE_USER", "ROLE_ADMIN"));
    }
}
