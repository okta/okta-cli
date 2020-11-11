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
package com.okta.cli.common.service;

import com.okta.sdk.client.Client;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.application.Application;
import com.okta.sdk.resource.application.ApplicationCredentialsOAuthClient;
import com.okta.sdk.resource.application.ApplicationGroupAssignment;
import com.okta.sdk.resource.application.OAuthApplicationCredentials;
import com.okta.sdk.resource.application.OAuthEndpointAuthenticationMethod;
import com.okta.sdk.resource.application.OAuthGrantType;
import com.okta.sdk.resource.application.OAuthResponseType;
import com.okta.sdk.resource.application.OpenIdConnectApplication;
import com.okta.sdk.resource.application.OpenIdConnectApplicationSettings;
import com.okta.sdk.resource.application.OpenIdConnectApplicationSettingsClient;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.okta.commons.lang.Collections.isEmpty;

class DefaultOidcAppCreator implements OidcAppCreator {

    @Override
    public ExtensibleResource createOidcApp(Client client, String oidcAppName, List<String> redirectUris, List<String> postLogoutRedirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            OpenIdConnectApplicationSettingsClient oauthClient = client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                    .setRedirectUris(redirectUris)
                    .setResponseTypes(Collections.singletonList(OAuthResponseType.CODE))
                    .setGrantTypes(Collections.singletonList(OAuthGrantType.AUTHORIZATION_CODE))
                    .setApplicationType(OpenIdConnectApplicationType.WEB);

            List<String> logoutRedirectUris = getOrDefaultWebBasedPostLogoutRedirectUris(postLogoutRedirectUris, redirectUris);
            if (!isEmpty(logoutRedirectUris)) {
                oauthClient.setPostLogoutRedirectUris(logoutRedirectUris);
            }

            Application app = client.instantiate(OpenIdConnectApplication.class)
                .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                    .setOAuthClient(oauthClient))
                .setLabel(oidcAppName);
            app = client.createApplication(app);
            assignAppToEveryoneGroup(client, app);

            return app;
        });

        // lookup the credentials for this application
        return getClientCredentials(client, oidcApplication);
    }

    @Override
    public ExtensibleResource createOidcNativeApp(Client client, String oidcAppName, List<String> redirectUris, List<String> postLogoutRedirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            OpenIdConnectApplicationSettingsClient oauthClient = client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                    .setRedirectUris(redirectUris)
                    .setResponseTypes(Collections.singletonList(OAuthResponseType.CODE))
                    .setGrantTypes(Collections.singletonList(OAuthGrantType.AUTHORIZATION_CODE))
                    .setApplicationType(OpenIdConnectApplicationType.NATIVE);

            Application app = client.instantiate(OpenIdConnectApplication.class)
                    .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(oauthClient))
                    .setLabel(oidcAppName)
                    .setCredentials(client.instantiate(OAuthApplicationCredentials.class)
                        .setOAuthClient(client.instantiate(ApplicationCredentialsOAuthClient.class)
                                .setTokenEndpointAuthMethod(OAuthEndpointAuthenticationMethod.NONE)));

            if (!isEmpty(postLogoutRedirectUris)) {
                oauthClient.setPostLogoutRedirectUris(postLogoutRedirectUris);
            }

            app = client.createApplication(app);
            assignAppToEveryoneGroup(client, app);

            return app;
        });

        // lookup the credentials for this application
        return getClientCredentials(client, oidcApplication);
    }

    @Override
    public ExtensibleResource createOidcSpaApp(Client client, String oidcAppName, List<String> redirectUris, List<String> postLogoutRedirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            OpenIdConnectApplicationSettingsClient oauthClient = client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                    .setRedirectUris(redirectUris)
                    .setResponseTypes(Collections.singletonList(OAuthResponseType.CODE))
                    .setGrantTypes(Collections.singletonList(OAuthGrantType.AUTHORIZATION_CODE))
                    .setApplicationType(OpenIdConnectApplicationType.BROWSER);

            List<String> logoutRedirectUris = getOrDefaultWebBasedPostLogoutRedirectUris(postLogoutRedirectUris, redirectUris);
            if (!isEmpty(logoutRedirectUris)) {
                oauthClient.setPostLogoutRedirectUris(logoutRedirectUris);
            }

            Application app = client.instantiate(OpenIdConnectApplication.class)
                    .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(oauthClient))
                    .setLabel(oidcAppName)
                    .setCredentials(client.instantiate(OAuthApplicationCredentials.class)
                            .setOAuthClient(client.instantiate(ApplicationCredentialsOAuthClient.class)
                                    .setTokenEndpointAuthMethod(OAuthEndpointAuthenticationMethod.NONE)));

            app = client.createApplication(app);
            assignAppToEveryoneGroup(client, app);

            return app;
        });

        // lookup the credentials for this application
        return getClientCredentials(client, oidcApplication);
    }

    @Override
    public ExtensibleResource createOidcServiceApp(Client client, String oidcAppName, List<String> redirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            Application app = client.instantiate(OpenIdConnectApplication.class)
                    .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                                    .setRedirectUris(redirectUris)
                                    .setResponseTypes(Collections.singletonList(OAuthResponseType.TOKEN))
                                    .setGrantTypes(Collections.singletonList(OAuthGrantType.CLIENT_CREDENTIALS))
                                    .setApplicationType(OpenIdConnectApplicationType.SERVICE)))
                    .setLabel(oidcAppName);

            app = client.createApplication(app);
            assignAppToEveryoneGroup(client, app);

            return app;
        });

        // lookup the credentials for this application
        return getClientCredentials(client, oidcApplication);
    }

    private Optional<Application> getApplication(Client client, String appName) {
        return client.listApplications(appName, null, null, null).stream()
                .filter(app -> appName.equalsIgnoreCase(app.getLabel()))
                .findFirst();
    }

    private ExtensibleResource getClientCredentials(Client client, Application application) {
        return client.http()
                .get("/api/v1/internal/apps/" + application.getId() + "/settings/clientcreds", ExtensibleResource.class);
    }

    private void assignAppToEveryoneGroup(Client client, Application app) {
        // look up 'everyone' group id
        String everyoneGroupId = client.listGroups("everyone", null).single().getId();

        ApplicationGroupAssignment aga = client.instantiate(ApplicationGroupAssignment.class).setPriority(2);
        app.createApplicationGroupAssignment(everyoneGroupId, aga);
    }

    private List<String> getOrDefaultWebBasedPostLogoutRedirectUris(List<String> postLogoutRedirectUris, List<String> redirectUris) {

        if (isEmpty(postLogoutRedirectUris)) {
                // default to using the redirect URIs base URL
                return redirectUris.stream()
                        .map(redirectUri -> {
                            URI uri = URI.create(redirectUri).resolve("/");
                            return uri.toString();
                        })
                        .distinct()
                        .collect(Collectors.toList());
        } else {
            return postLogoutRedirectUris;
        }
    }
}