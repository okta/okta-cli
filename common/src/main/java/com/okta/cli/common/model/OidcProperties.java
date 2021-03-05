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

import java.net.URI;
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

    public static SpringOidcProperties spring() {
        return spring("oidc");
    }

    public static SpringOidcProperties spring(String tenantId) {
        return new SpringOidcProperties(tenantId);
    }

    public static QuarkusOidcProperties quarkus(OpenIdConnectApplicationType applicationType) {
        return quarkus(applicationType, false);
    }

    public static QuarkusOidcProperties quarkus(OpenIdConnectApplicationType applicationType, boolean jhipster) {
        return new QuarkusOidcProperties(applicationType, jhipster);
    }

    public static MicronautOidcProperties micronaut() {
        return OidcProperties.micronaut(OpenIdConnectApplicationType.SERVICE);
    }

    public static MicronautOidcProperties micronaut(OpenIdConnectApplicationType applicationType) {
        return micronaut("oidc", applicationType);
    }

    public static MicronautOidcProperties micronaut(String tenantId, OpenIdConnectApplicationType applicationType) {
        return new MicronautOidcProperties(tenantId, applicationType);
    }

    public final String issuerUriPropertyName;
    public final String clientIdPropertyName;
    public final String clientSecretPropertyName;

    String issuerUri;
    String clientId;
    String clientSecret;
    List<String> redirectUris;
    List<String> postLogoutUris;

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

    public void setPostLogoutUris(List<String> postLogoutUris) {
        this.postLogoutUris = postLogoutUris;
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
        private final OpenIdConnectApplicationType applicationType;
        private final boolean jhipster;

        public QuarkusOidcProperties(OpenIdConnectApplicationType applicationType, boolean jhipster) {
            super(
                    "quarkus.oidc.auth-server-url",
                    "quarkus.oidc.client-id",
                    "quarkus.oidc.credentials.secret"
            );
            this.applicationType = applicationType;
            this.jhipster = jhipster;
        }

        @Override
        Map<String, String> getOidcClientProperties() {
            if (applicationType == OpenIdConnectApplicationType.WEB) {
                String redirectUri = "/";
                if (redirectUris != null && !redirectUris.isEmpty()) {
                    redirectUri = redirectUris.get(0);
                }

                Map<String, String> props = new LinkedHashMap<>();
                props.put("quarkus.oidc.authentication.redirect-path", URI.create(redirectUri).getPath());

                if (jhipster) {
                    // If we need other jhipster properties, it might be better to create a JHipsterOidcProperties that
                    // wraps other implementations, and adds any needed properties like this
                    props.put("jhipster.oidc.logout-url", issuerUri + "/v1/logout");
                } else {
                    props.put("quarkus.oidc.application-type", "web-app");
                }
                return props;
            } else {
                return Map.of("quarkus.oidc.application-type", "service");
            }
        }
    }

    public static class MicronautOidcProperties extends OidcProperties {

        private final OpenIdConnectApplicationType applicationType;

        public MicronautOidcProperties(String tenantId, OpenIdConnectApplicationType applicationType) {
            super(
                format("micronaut.security.oauth2.clients.%s.openid.issuer", tenantId),
                format("micronaut.security.oauth2.clients.%s.client-id", tenantId),
                format("micronaut.security.oauth2.clients.%s.client-secret", tenantId)
            );
            this.applicationType = applicationType;
        }

        @Override
        Map<String, String> getOidcClientProperties() {
            if (applicationType == OpenIdConnectApplicationType.WEB) {
                return Map.of(
                        "micronaut.security.oauth2.callback-uri", getFirstUriPath(redirectUris),
                        "micronaut.security.endpoints.logout.path", getFirstUriPath(postLogoutUris)
                );
            } else {
                return Collections.emptyMap();
            }
        }

        private static String getFirstUriPath(List<String> uris) {
            if (uris != null && !uris.isEmpty()) {
                return URI.create(uris.get(0)).getPath();
            }

            return "/";
        }
    }
}
