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
package com.okta.cli.common.service;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.cli.common.model.RegistrationQuestions;
import com.okta.sdk.client.Client;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface SetupService {

    // Copied from OpenIdConnectApplicationType, device_code is missing from that class
    String APP_TYPE_WEB = "web";
    String APP_TYPE_SERVICE = "service";
    String APP_TYPE_BROWSER = "browser";
    String APP_TYPE_NATIVE = "native";
    String APP_TYPE_DEVICE = "urn:ietf:params:oauth:grant-type:device_code";

    OrganizationResponse createOktaOrg(RegistrationQuestions registrationQuestions,
                                       File oktaPropsFile,
                                       boolean demo,
                                       boolean interactive) throws IOException, ClientConfigurationException, UserCanceledException;

    void verifyOktaOrg(String identifier,
                       RegistrationQuestions registrationQuestions,
                       File oktaPropsFile) throws IOException, ClientConfigurationException;

    default void createOidcApplication(MutablePropertySource propertySource,
                                       String oidcAppName,
                                       String orgUrl,
                                       String groupClaimName,
                                       Set<String> groupsToCreate,
                                       String issuerUri,
                                       String authorizationServerId,
                                       boolean interactive,
                                       String appType,
                                       Client client) throws IOException {
        createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, groupsToCreate, issuerUri, authorizationServerId, interactive, appType, Collections.emptyList(), client);
    }

    default void createOidcApplication(MutablePropertySource propertySource,
                                       String oidcAppName,
                                       String orgUrl,
                                       String groupClaimName,
                                       Set<String> groupsToCreate,
                                       String issuerUri,
                                       String authorizationServerId,
                                       boolean interactive,
                                       String appType,
                                       List<String> redirectUris,
                                       Client client) throws IOException {
        createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, groupsToCreate, issuerUri, authorizationServerId, interactive, appType, redirectUris, Collections.emptyList(), client);
    }

    default void createOidcApplication(MutablePropertySource propertySource,
                                       String oidcAppName,
                                       String orgUrl,
                                       String groupClaimName,
                                       Set<String> groupsToCreate,
                                       String issuerUri,
                                       String authorizationServerId,
                                       boolean interactive,
                                       String appType,
                                       List<String> redirectUris,
                                       List<String> postLogoutRedirectUris,
                                       Client client) throws IOException {
        createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, groupsToCreate, issuerUri, authorizationServerId, interactive, appType, redirectUris, postLogoutRedirectUris, Collections.emptyList(), client);
    }

    void createOidcApplication(MutablePropertySource propertySource,
                               String oidcAppName,
                               String orgUrl,
                               String groupClaimName,
                               Set<String> groupsToCreate,
                               String issuerUri,
                               String authorizationServerId,
                               boolean interactive,
                               String appType,
                               List<String> redirectUris,
                               List<String> postLogoutRedirectUris,
                               List<String> trustedOrigins,
                               Client client) throws IOException;
}
