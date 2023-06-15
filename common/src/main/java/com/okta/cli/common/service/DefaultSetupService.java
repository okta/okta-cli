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
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.impl.resource.DefaultGroupBuilder;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.ResourceException;
import com.okta.sdk.resource.authorization.server.policy.AuthorizationServerPolicyRule;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultSetupService implements SetupService {

    private final SdkConfigurationService sdkConfigurationService;

    private final OktaOrganizationCreator organizationCreator;

    private final OidcAppCreator oidcAppCreator;

    private final AuthorizationServerService authorizationServerService;

    private final OidcProperties oidcProperties;

    private final Duration verificationPollingFrequency = Duration.ofSeconds(4);

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
                                              boolean interactive) throws IOException, ClientConfigurationException, UserCanceledException {


        // check if okta client config exists?
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();

        try (ProgressBar progressBar = ProgressBar.create(interactive)) {

            if (!Strings.isEmpty(clientConfiguration.getBaseUrl())) {
                if (!registrationQuestions.isOverwriteExistingConfig(clientConfiguration.getBaseUrl(), oktaPropsFile.getAbsolutePath())) {
                    throw new UserCanceledException();
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
                progressBar.info("An account activation email has been sent to you.");
                return newOrg;
            } catch (RestException | ResourceException e) {
                throw new ClientConfigurationException(e.getMessage() +
                                                       // the original REST exception likely contained information about why this failed
                                                       "\nFailed to create Okta Organization. You can register manually by going " +
                                                       "to https://developer.okta.com/signup", e);
            }
        }
    }

    @Override
    public void verifyOktaOrg(String identifier, RegistrationQuestions registrationQuestions, File oktaPropsFile) throws IOException {

        try (ProgressBar progressBar = ProgressBar.create(true)) {

            progressBar.info("Check your email");

            OrganizationResponse response = null;
            while(response == null || !response.isActive()) {
                try {
                    // poll
                    Thread.sleep(verificationPollingFrequency.toMillis());
                    response = organizationCreator.verifyNewOrg(identifier);
                } catch (RestException | InterruptedException | ResourceException e) {
                    String error = "Failed to verify new Okta Organization. If you have already registered " +
                                   "use \"okta login\" to configure the Okta CLI";
                    progressBar.info(error);
                    throw new IllegalStateException(error, e);
                }
            }

            sdkConfigurationService.writeOktaYaml(response.getOrgUrl(), response.getApiToken(), oktaPropsFile);

            progressBar.info("New Okta Account created!");
            progressBar.info("Your Okta Domain: "+ response.getOrgUrl());

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
                                      String appType,
                                      List<String> redirectUris,
                                      List<String> postLogoutRedirectUris,
                                      List<String> trustedOrigins,
                                      Client client) throws IOException {

        // Create new Application
        String clientId = propertySource.getProperty(oidcProperties.clientIdPropertyName);
        boolean enableDeviceGrant = false;

        try (ProgressBar progressBar = ProgressBar.create(interactive)) {
            if (!ConfigurationValidator.validateClientId(clientId).isValid()) {

                progressBar.start("Configuring a new OIDC Application, almost done:");

                ExtensibleResource clientCredsResponse;
                switch (appType) {
                    case APP_TYPE_WEB:
                        clientCredsResponse = oidcAppCreator.createOidcApp(client, oidcAppName, redirectUris, postLogoutRedirectUris);
                        break;
                    case APP_TYPE_NATIVE:
                        clientCredsResponse = oidcAppCreator.createOidcNativeApp(client, oidcAppName, redirectUris, postLogoutRedirectUris);
                        break;
                    case APP_TYPE_BROWSER:
                        clientCredsResponse = oidcAppCreator.createOidcSpaApp(client, oidcAppName, redirectUris, postLogoutRedirectUris);
                        break;
                    case APP_TYPE_SERVICE:
                        clientCredsResponse = oidcAppCreator.createOidcServiceApp(client, oidcAppName, redirectUris);
                        break;
                    case APP_TYPE_DEVICE:
                        clientCredsResponse = oidcAppCreator.createDeviceCodeApp(client, oidcAppName);
                        enableDeviceGrant = true;
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

                if (enableDeviceGrant) {
                    progressBar.info("Enabling Device Grant");
                    Optional<AuthorizationServerPolicyRule> optionalRule = authorizationServerService.getSinglePolicyRule(client, authorizationServerId);
                    optionalRule.ifPresentOrElse(rule -> {
                        authorizationServerService.enableDeviceGrant(client, authorizationServerId, rule);
                    },() -> {
                        progressBar.info("Custom Authorization Server policy detected, if you are going to use an Okta Custom Authorization Server, you must enable the 'Device Authorization' grant manually, see:");
                        progressBar.info("https://developer.okta.com/docs/guides/device-authorization-grant/main/#configure-the-authorization-server-policy-rule-for-device-authorization");
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
