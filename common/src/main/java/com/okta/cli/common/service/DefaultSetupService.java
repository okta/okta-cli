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

import com.okta.cli.common.FactorVerificationException;
import com.okta.cli.common.RestException;
import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.OidcProperties;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.cli.common.model.RegistrationQuestions;
import com.okta.cli.common.progressbar.ProgressBar;
import com.okta.commons.configcheck.ConfigurationValidator;
import com.okta.commons.lang.Collections;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.impl.resource.DefaultGroupBuilder;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import com.okta.sdk.resource.group.Group;
import com.okta.sdk.resource.group.GroupList;
import com.okta.sdk.resource.role.Scope;
import com.okta.sdk.resource.role.ScopeType;
import com.okta.sdk.resource.trusted.origin.TrustedOrigin;
import com.okta.sdk.resource.trusted.origin.TrustedOriginList;
import com.okta.sdk.resource.user.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultSetupService implements SetupService {

    private final SdkConfigurationService sdkConfigurationService;

    private final OktaOrganizationCreator organizationCreator;

    private final OidcAppCreator oidcAppCreator;

    private final AuthorizationServerService authorizationServerService;

    private final OidcProperties oidcProperties;


    public DefaultSetupService(OidcProperties oidcProperties) {
        this(new DefaultSdkConfigurationService(),
                new DefaultOktaOrganizationCreator(),
                new DefaultOidcAppCreator(),
                new DefaultAuthorizationServerService(),
                oidcProperties);
    }

    public DefaultSetupService(SdkConfigurationService sdkConfigurationService,
                               OktaOrganizationCreator organizationCreator,
                               OidcAppCreator oidcAppCreator, 
                               AuthorizationServerService authorizationServerService,
                               OidcProperties oidcProperties) {
        this.sdkConfigurationService = sdkConfigurationService;
        this.organizationCreator = organizationCreator;
        this.oidcAppCreator = oidcAppCreator;
        this.authorizationServerService = authorizationServerService;
        this.oidcProperties = oidcProperties;
    }

    @Override
    public OrganizationResponse createOktaOrg(RegistrationQuestions registrationQuestions,
                                              File oktaPropsFile,
                                              boolean demo,
                                              boolean interactive) throws IOException, ClientConfigurationException {


        // check if okta client config exists?
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();

        String orgUrl;
        try (ProgressBar progressBar = ProgressBar.create(interactive)) {

            if (!Strings.isEmpty(clientConfiguration.getBaseUrl())) {
                progressBar.info("An existing Okta Organization (" + clientConfiguration.getBaseUrl() + ") was found in "+ oktaPropsFile.getAbsolutePath());

                if (!registrationQuestions.isOverwriteConfig()) {
                    throw new ClientConfigurationException("User canceled");
                }

                Instant instant = Instant.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "uuuuMMdd'T'HHmm" ).withZone(ZoneId.of("UTC"));

                File backupFile = new File(oktaPropsFile.getParent(), oktaPropsFile.getName() + "." + formatter.format(instant));
                Files.copy(oktaPropsFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                progressBar.info("Configuration file backed: "+ backupFile.getAbsolutePath());
            }

            // resolve the request (potentially prompt for input) before starting the progress bar
            OrganizationRequest organizationRequest = registrationQuestions.getOrganizationRequest();
            progressBar.start("Creating new Okta Organization, this may take a minute:");

            try {
                OrganizationResponse newOrg = organizationCreator.createNewOrg(organizationRequest);
                orgUrl = newOrg.getOrgUrl();

                progressBar.info("OrgUrl: " + orgUrl);
                progressBar.info("An email has been sent to you with a verification code.");
                return newOrg;
            } catch (RestException e) {
                throw new ClientConfigurationException("Failed to create Okta Organization. You can register " +
                                                       "manually by going to https://developer.okta.com/signup");
            }
        }
    }


    @Override
    public void verifyOktaOrg(String identifier, RegistrationQuestions registrationQuestions, File oktaPropsFile) throws IOException, ClientConfigurationException {

        try (ProgressBar progressBar = ProgressBar.create(true)) {

            progressBar.info("Check your email");

            OrganizationResponse response = null;
            while(response == null) {
                try {
                    // prompt for code
                    String code = registrationQuestions.getVerificationCode();
                    response = organizationCreator.verifyNewOrg(identifier, code);
                } catch (FactorVerificationException e) {
                    progressBar.info("Invalid Passcode, try again.");
                }
            }
            // TODO handle polling in case the org is not ready

            sdkConfigurationService.writeOktaYaml(response.getOrgUrl(), response.getApiToken(), oktaPropsFile);

            progressBar.info("New Okta Account created!");
            progressBar.info("Your Okta Domain: "+ response.getOrgUrl());
            progressBar.info("To set your password open this link:\n" + response.getUpdatePasswordUrl());

            // TODO demo mode?
        }
    }

    @Override
    public void createOidcApplication(MutablePropertySource propertySource,
                                      String oidcAppName,
                                      String orgUrl,
                                      String groupClaimName,
                                      Set<String> groupsToCreate,
                                      String issuerUri,
                                      String authorizationServerId,
                                      boolean interactive,
                                      OpenIdConnectApplicationType appType,
                                      List<String> redirectUris,
                                      List<String> postLogoutRedirectUris,
                                      List<String> trustedOrigins) throws IOException {

        // Create new Application
        String clientId = propertySource.getProperty(oidcProperties.clientIdPropertyName);

        try (ProgressBar progressBar = ProgressBar.create(interactive)) {
            if (!ConfigurationValidator.validateClientId(clientId).isValid()) {

                progressBar.start("Configuring a new OIDC Application, almost done:");

                // create ODIC application
                Client client = Clients.builder().build();

                ExtensibleResource clientCredsResponse;
                switch (appType) {
                    case WEB:
                        clientCredsResponse = oidcAppCreator.createOidcApp(client, oidcAppName, redirectUris, postLogoutRedirectUris);
                        break;
                    case NATIVE:
                        clientCredsResponse = oidcAppCreator.createOidcNativeApp(client, oidcAppName, redirectUris, postLogoutRedirectUris);
                        break;
                    case BROWSER:
                        clientCredsResponse = oidcAppCreator.createOidcSpaApp(client, oidcAppName, redirectUris, postLogoutRedirectUris);
                        break;
                    case SERVICE:
                        clientCredsResponse = oidcAppCreator.createOidcServiceApp(client, oidcAppName, redirectUris);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported Application Type: "+ appType);
                }

                if (Strings.isEmpty(issuerUri)) {
                    issuerUri = orgUrl + "/oauth2/" + authorizationServerId;
                }

                oidcProperties.setIssuerUri(issuerUri);
                oidcProperties.setClientId(clientCredsResponse.getString("client_id"));
                oidcProperties.setClientSecret(clientCredsResponse.getString("client_secret"));
                oidcProperties.setRedirectUris(redirectUris);
                oidcProperties.setPostLogoutUris(postLogoutRedirectUris);

                propertySource.addProperties(oidcProperties.getProperties());

                progressBar.info("Created OIDC application, client-id: " + clientCredsResponse.getString("client_id"));

                if (!Strings.isEmpty(groupClaimName)) {
                    progressBar.info("Creating Authorization Server claim '" + groupClaimName + "':");
                    authorizationServerService.createGroupClaim(client, groupClaimName, authorizationServerId);
                }

                if (!Collections.isEmpty(groupsToCreate)) {
                    User user = client.getUser("me"); // The user the owns the api token
                    progressBar.info("Adding user '" + user.getProfile().getLogin() + "' to groups: " + groupsToCreate);
                    groupsToCreate.forEach(groupName -> {
                        createAndAssignGroup(client, user, groupName, progressBar);
                    });
                }

                // configure trusted origins
                configureTrustedOrigins(client, trustedOrigins);
            } else {
                progressBar.info("Existing OIDC application detected for clientId: "+ clientId + ", skipping new application creation\n");
            }
        }
    }

    private void createAndAssignGroup(Client client, User user, String groupName, ProgressBar progressBar) {

        GroupList groups  = client.listGroups(groupName, null, null);
        Group group = groups.stream()
                .filter(it -> it.getProfile().getName().equals(groupName))
                .findFirst()
                .map(it -> {
                    // a bit of abuse for map(), but it removes the need for a few other branches
                    progressBar.info("Existing group '" + groupName + "' found");
                    return it;
                })
                .orElseGet(() -> {
                    progressBar.info("Creating group: " + groupName);
                    return new DefaultGroupBuilder()
                            .setName(groupName)
                            .buildAndCreate(client);
                });

        user.addToGroup(group.getId());
    }

    private void configureTrustedOrigins(Client client, List<String> trustedOrigins) {
        // Configure CORS if needed
        if (!Collections.isEmpty(trustedOrigins)) {

            TrustedOriginList origins = client.listOrigins();

            List<Scope> scopes = List.of(
                client.instantiate(Scope.class).setType(ScopeType.CORS),
                client.instantiate(Scope.class).setType(ScopeType.REDIRECT)
            );

            trustedOrigins.forEach(url -> {
                origins.stream()
                        .filter(trustedOrigin -> url.equals(trustedOrigin.getOrigin()))

                        // origins are unique, so just grab the first
                        .findFirst().ifPresentOrElse(trustedOrigin -> {

                            // nested object, just get the enum in a set
                            Set<ScopeType> scopesSet = trustedOrigin.getScopes().stream()
                                    .map(Scope::getType)
                                    .collect(Collectors.toSet());

                            // if either is missing enable both of them
                            if (!scopesSet.contains(ScopeType.CORS) || !scopesSet.contains(ScopeType.REDIRECT)) {

                                // Add CORS and Redirect to origin
                                trustedOrigin.setScopes(scopes);
                                trustedOrigin.update();
                            }
                        }, () -> {
                            // create a new trusted origin if it doesn't exist
                            client.createOrigin(client.instantiate(TrustedOrigin.class)
                                    .setOrigin(url)
                                    .setName(url)
                                    .setScopes(scopes));
                        }
                );
            });
        }
    }
}
