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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

public class DefaultOidcAppCreator implements OidcAppCreator {

    @Override
    public ExtensibleResource createOidcApp(Client client, String oidcAppName, String... redirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            Application app = client.instantiate(OpenIdConnectApplication.class)
                .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                    .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                        .setRedirectUris(Arrays.asList(redirectUris))
                        .setResponseTypes(Collections.singletonList(OAuthResponseType.CODE))
                        .setGrantTypes(Collections.singletonList(OAuthGrantType.AUTHORIZATION_CODE))
                        .setApplicationType(OpenIdConnectApplicationType.WEB)))
                .setLabel(oidcAppName);
            app = client.createApplication(app);
            assignAppToEveryoneGroup(client, app);

            return app;
        });

        // lookup the credentials for this application
        return getClientCredentials(client, oidcApplication);
    }

    @Override
    public ExtensibleResource createOidcNativeApp(Client client, String oidcAppName, String... redirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            Application app = client.instantiate(OpenIdConnectApplication.class)
                    .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                                    .setRedirectUris(Arrays.asList(redirectUris))
                                    .setResponseTypes(Collections.singletonList(OAuthResponseType.CODE))
                                    .setGrantTypes(Collections.singletonList(OAuthGrantType.AUTHORIZATION_CODE))
                                    .setApplicationType(OpenIdConnectApplicationType.NATIVE)))
                    .setLabel(oidcAppName)
                    .setCredentials(client.instantiate(OAuthApplicationCredentials.class)
                            .setOAuthClient(client.instantiate(ApplicationCredentialsOAuthClient.class)
                            .setTokenEndpointAuthMethod(OAuthEndpointAuthenticationMethod.NONE)));

            // TODO post_logout_redirect_uris

            app = client.createApplication(app);
            assignAppToEveryoneGroup(client, app);

            return app;
        });

        // lookup the credentials for this application
        return getClientCredentials(client, oidcApplication);
    }

    @Override
    public ExtensibleResource createOidcSpaApp(Client client, String oidcAppName, String... redirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            Application app = client.instantiate(OpenIdConnectApplication.class)
                    .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                                    .setRedirectUris(Arrays.asList(redirectUris))
                                    .setResponseTypes(Collections.singletonList(OAuthResponseType.CODE))
                                    .setGrantTypes(Collections.singletonList(OAuthGrantType.AUTHORIZATION_CODE))
                                    .setApplicationType(OpenIdConnectApplicationType.BROWSER)))
                    .setLabel(oidcAppName)
                    .setCredentials(client.instantiate(OAuthApplicationCredentials.class)
                            .setOAuthClient(client.instantiate(ApplicationCredentialsOAuthClient.class)
                                    .setTokenEndpointAuthMethod(OAuthEndpointAuthenticationMethod.NONE)));

            // TODO post_logout_redirect_uris

            app = client.createApplication(app);
            assignAppToEveryoneGroup(client, app);

            return app;
        });

        // lookup the credentials for this application
        return getClientCredentials(client, oidcApplication);
    }

    @Override
    public ExtensibleResource createOidcServiceApp(Client client, String oidcAppName, String... redirectUris) {

        Optional<Application> existingApp = getApplication(client, oidcAppName);

        // create a new OIDC app if one does NOT exist
        Application oidcApplication = existingApp.orElseGet(() -> {

            Application app = client.instantiate(OpenIdConnectApplication.class)
                    .setSettings(client.instantiate(OpenIdConnectApplicationSettings.class)
                            .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient.class)
                                    .setRedirectUris(Arrays.asList(redirectUris))
                                    .setResponseTypes(Collections.singletonList(OAuthResponseType.TOKEN))
                                    .setGrantTypes(Collections.singletonList(OAuthGrantType.CLIENT_CREDENTIALS))
                                    .setApplicationType(OpenIdConnectApplicationType.SERVICE)))
                    .setLabel(oidcAppName);

            // TODO post_logout_redirect_uris

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
        String everyoneGroupId = client.listGroups("everyone", null, null).single().getId();

        ApplicationGroupAssignment aga = client.instantiate(ApplicationGroupAssignment.class).setPriority(2);
        app.createApplicationGroupAssignment(everyoneGroupId, aga);
    }
}