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
package com.okta.cli.common.model;

import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public abstract class OidcProperties {

    public static final Logger logger = LoggerFactory.getLogger(OidcProperties.class);

    public static OktaEnvOidcProperties oktaEnv() {
        return new OktaEnvOidcProperties();
    }

    public static OidcProperties jhipster(OpenIdConnectApplicationType applicationType) {

        File currentDir = new File(System.getProperty("user.dir")).getAbsoluteFile();

        // jhipster is a generator, so the underlying project could be spring, quarkus, or something else
        // attempt to figure out the delegate but fallback to the default spring impl
        if (currentDir.exists()) {
            File packageJsonFile = new File(currentDir, "package.json");
            if (packageJsonFile.exists()) {
                try {
                    String packageJson = Files.readString(packageJsonFile.toPath());
                    if (packageJson.contains("generator-jhipster-quarkus")) {
                        return quarkus(applicationType);
                    } else if (packageJson.contains("generator-jhipster-micronaut")) {
                        return micronaut("oidc");
                    } // add other JHipster implementations here

                } catch (IOException e) {
                    // log the error, fallback to spring impl
                    logger.warn("Failed to parse: {}", packageJsonFile.getAbsolutePath(), e);
                }
            }
        }
        return spring("oidc");
    }

    public static SpringOidcProperties spring() {
        return spring("oidc");
    }

    public static SpringOidcProperties spring(String tenantId) {
        return new SpringOidcProperties(tenantId);
    }

    public static QuarkusOidcProperties quarkus() {
        return quarkus(OpenIdConnectApplicationType.SERVICE);
    }

    public static QuarkusOidcProperties quarkus(OpenIdConnectApplicationType applicationType) {
        return new QuarkusOidcProperties(applicationType);
    }

    public static MicronautOidcProperties micronaut() {
        return micronaut("oidc");
    }

    public static MicronautOidcProperties micronaut(String tenantId) {
        return new MicronautOidcProperties(tenantId);
    }

    public final String issuerUriPropertyName;
    public final String clientIdPropertyName;
    public final String clientSecretPropertyName;

    String issuerUri;
    String clientId;
    String clientSecret;
    List<String> redirectUris;

    OidcProperties(String issuerUriPropertyName, String clientIdPropertyName, String clientSecretPropertyName) {
        this.issuerUriPropertyName = issuerUriPropertyName;
        this.clientIdPropertyName = clientIdPropertyName;
        this.clientSecretPropertyName = clientSecretPropertyName;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    abstract Map<String, String> getOidcClientProperties();

    public Map<String, String> getProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(issuerUriPropertyName, issuerUri);
        properties.put(clientIdPropertyName, clientId);
        properties.put(clientSecretPropertyName, clientSecret);
        properties.putAll(getOidcClientProperties());

        return properties;
    }

    public static class SpringOidcProperties extends OidcProperties {
        public SpringOidcProperties(String tenantId) {
            super(
                    format("spring.security.oauth2.client.provider.%s.issuer-uri", tenantId),
                    format("spring.security.oauth2.client.registration.%s.client-id", tenantId),
                    format("spring.security.oauth2.client.registration.%s.client-secret", tenantId)
            );
        }

        @Override
        Map<String, String> getOidcClientProperties() {
            return Collections.emptyMap();
        }
    }

    public static class OktaEnvOidcProperties extends OidcProperties {
        public OktaEnvOidcProperties() {
            super(
                    "okta.oauth2.issuer",
                    "okta.oauth2.client-id",
                    "okta.oauth2.client-secret"
            );
        }

        @Override
        Map<String, String> getOidcClientProperties() {
            return Collections.emptyMap();
        }
    }

    public static class QuarkusOidcProperties extends OidcProperties {
        public final String applicationType;

        public QuarkusOidcProperties(OpenIdConnectApplicationType applicationType) {
            super(
                    "quarkus.oidc.auth-server-url",
                    "quarkus.oidc.client-id",
                    "quarkus.oidc.credentials.secret"
            );
            if (applicationType == OpenIdConnectApplicationType.WEB) {
                this.applicationType = "web-app";
            } else {
                this.applicationType = "service";
            }
        }

        @Override
        Map<String, String> getOidcClientProperties() {
            String redirectUri = "/";
            if (redirectUris != null && !redirectUris.isEmpty()) {
                redirectUri = redirectUris.get(0);
            }

            String redirectPath = URI.create(redirectUri).getPath();
            Map<String, String> props = new LinkedHashMap<>();
            props.put("quarkus.oidc.application-type", applicationType);
            props.put("quarkus.oidc.authentication.redirect-path", redirectPath);

            // detect if it's a JHipster app
            if (redirectPath.endsWith("/login/oauth2/code/oidc")) {
                props.put("jhipster.oidc.logout-url", issuerUri + "/v1/logout");
            }

            return props;
        }
    }

    public static class MicronautOidcProperties extends OidcProperties {
        public MicronautOidcProperties(String tenantId) {
            super(
                format("micronaut.security.oauth2.clients.%s.openid.issuer", tenantId),
                format("micronaut.security.oauth2.clients.%s.client-id", tenantId),
                format("micronaut.security.oauth2.clients.%s.client-secret", tenantId)
            );
        }

        @Override
        Map<String, String> getOidcClientProperties() {
            String redirectUri = "/";
            if (redirectUris != null && !redirectUris.isEmpty()) {
                redirectUri = redirectUris.get(0);
            }

            String redirectPath = URI.create(redirectUri).getPath();
            // detect if it's a JHipster app
            if (redirectPath.endsWith("/login/oauth2/code/oidc")) {
                return Map.of(
                    "micronaut.security.oauth2.callback-uri", "/login/oauth2/code{/provider}"
                );
            } else {
                return Collections.emptyMap();
            }

        }
    }
}
