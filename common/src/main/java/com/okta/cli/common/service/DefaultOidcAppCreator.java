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
import com.okta.sdk.resource.application.ApplicationGroupAssignment;
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

        Optional<Application> existingApp = client.listApplications(oidcAppName, null, null, null).stream()
                .filter(app -> oidcAppName.equalsIgnoreCase(app.getLabel()))
                .findFirst();

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

            // assign Everyone group to new app
            // look up 'everyone' group id
            String everyoneGroupId = client.listGroups("everyone", null, null).single().getId();

            ApplicationGroupAssignment aga = client.instantiate(ApplicationGroupAssignment.class).setPriority(2);
            app.createApplicationGroupAssignment(everyoneGroupId, aga);

            return app;
        });

        // lookup the credentials for this application
        return client.http()
            .get("/api/v1/internal/apps/" + oidcApplication.getId() + "/settings/clientcreds", ExtensibleResource.class);
    }
}