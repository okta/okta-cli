package com.okta.cli.common.service;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.cli.common.progressbar.ProgressBar;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.resource.ExtensibleResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class DefaultSetupService implements SetupService {

    private final SdkConfigurationService sdkConfigurationService;

    private final OktaOrganizationCreator organizationCreator;

    private final OidcAppCreator oidcAppCreator;

    private final AuthorizationServerConfigureService authorizationServerConfigureService;

    private final String springPropertyKey;

    /**
     * The base URL of the service used to create a new Okta account.
     * This value is NOT exposed as a plugin parameter, but CAN be set using the system property {@code okta.maven.apiBaseUrl}.
     */
    private String apiBaseUrl = "https://start.okta.dev/";

    private final Logger logger = LoggerFactory.getLogger(DefaultSetupService.class);

    public DefaultSetupService(String springPropertyKey) {
        this(new DefaultSdkConfigurationService(),
                new DefaultOktaOrganizationCreator(),
                new DefaultOidcAppCreator(),
                new DefaultAuthorizationServerConfigureService(),
                springPropertyKey);
    }

    public DefaultSetupService(SdkConfigurationService sdkConfigurationService,
                               OktaOrganizationCreator organizationCreator,
                               OidcAppCreator oidcAppCreator, 
                               AuthorizationServerConfigureService authorizationServerConfigureService,
                               String springPropertyKey) {
        this.sdkConfigurationService = sdkConfigurationService;
        this.organizationCreator = organizationCreator;
        this.oidcAppCreator = oidcAppCreator;
        this.authorizationServerConfigureService = authorizationServerConfigureService;
        this.springPropertyKey = springPropertyKey;
    }

    @Override
    public void configureEnvironment(
            Supplier<OrganizationRequest> organizationRequestSupplier,
            File oktaPropsFile,
            MutablePropertySource propertySource,
            String oidcAppName,
            String groupClaimName,
            String authorizationServerId,
            boolean demo,
            boolean interactive,
            String... redirectUris) throws IOException, ClientConfigurationException {

            // get current or sign up for new org
            String orgUrl = createOktaOrg(organizationRequestSupplier, oktaPropsFile, demo, interactive);

            // Create new Application
            createOidcApplication(propertySource, oidcAppName, orgUrl, groupClaimName, authorizationServerId, interactive, redirectUris);

    }

    @Override
    public String createOktaOrg(Supplier<OrganizationRequest> organizationRequestSupplier,
                                File oktaPropsFile,
                                boolean demo,
                                boolean interactive) throws IOException, ClientConfigurationException {


        // check if okta client config exists?
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();

        String orgUrl;
        try (ProgressBar progressBar = ProgressBar.create(interactive)) {
            if (Strings.isEmpty(clientConfiguration.getBaseUrl())) {

                // resolve the request (potentially prompt for input) before starting the progress bar
                OrganizationRequest organizationRequest = organizationRequestSupplier.get();
                progressBar.start("Creating new Okta Organization, this may take a minute:");

                OrganizationResponse newOrg = organizationCreator.createNewOrg(getApiBaseUrl(), organizationRequest);
                orgUrl = newOrg.getOrgUrl();

                progressBar.info("OrgUrl: " + orgUrl);
                progressBar.info("Check your email address to verify your account.\n");

                progressBar.info("Writing Okta SDK config to: " + oktaPropsFile.getAbsolutePath());
                // write ~/.okta/okta.yaml
                sdkConfigurationService.writeOktaYaml(orgUrl, newOrg.getApiToken(), oktaPropsFile);
            } else {
                if (demo) { // always prompt for user info in "demo mode", this info will not be used but it makes for a more realistic demo
                    organizationRequestSupplier.get();
                }

                orgUrl = clientConfiguration.getBaseUrl();
                progressBar.info("Current OrgUrl: " + clientConfiguration.getBaseUrl());

            }
        }
        return orgUrl;
    }

    @Override
    public void createOidcApplication(MutablePropertySource propertySource,
                                      String oidcAppName,
                                      String orgUrl,
                                      String groupClaimName,
                                      String authorizationServerId,
                                      boolean interactive,
                                      String... redirectUris) throws IOException {

        // Create new Application
        String clientId = propertySource.getProperty(getClientIdPropertyName());

        try (ProgressBar progressBar = ProgressBar.create(interactive)) {
            if (Strings.isEmpty(clientId)) {

                progressBar.start("Configuring a new OIDC Application, almost done:");

                // create ODIC application
                Client client = Clients.builder().build();

                ExtensibleResource clientCredsResponse = oidcAppCreator.createOidcApp(client, oidcAppName, redirectUris);

                Map<String, String> newProps = new HashMap<>();
                newProps.put(getIssuerUriPropertyName(), orgUrl + "/oauth2/" + authorizationServerId);
                newProps.put(getClientIdPropertyName(), clientCredsResponse.getString("client_id"));
                newProps.put(getClientSecretPropertyName(), clientCredsResponse.getString("client_secret"));

                propertySource.addProperties(newProps);

                progressBar.info("Created OIDC application, client-id: " + clientCredsResponse.getString("client_id"));

                if (!Strings.isEmpty(groupClaimName)) {

                    progressBar.info("Creating Authorization Server claim '" + groupClaimName + "':");
                    boolean asCreated = authorizationServerConfigureService.createGroupClaim(client, groupClaimName, authorizationServerId);
                    if (!asCreated) {
                        logger.warn("Could not create an Authorization Server claim with the name of '" + groupClaimName + "', it likely already exists. You can verify this in your Okta Admin console.");
                    }
                }
            } else {
                progressBar.info("Existing OIDC application detected for clientId: "+ clientId + ", skipping new application creation\n");
            }
        }
    }

    public String getApiBaseUrl() {
        return System.getenv().getOrDefault("OKTA_CLI_BASE_URL", apiBaseUrl);
    }

    private String getIssuerUriPropertyName() {
        return Optional.ofNullable(springPropertyKey)
                .map(id -> "spring.security.oauth2.client.provider." + id + ".issuer-uri")
                .orElse("okta.oauth2.issuer");
    }

    private String getClientIdPropertyName() {
        return Optional.ofNullable(springPropertyKey)
                .map(id -> "spring.security.oauth2.client.registration." + id + ".client-id")
                .orElse("okta.oauth2.client-id");
    }

    private String getClientSecretPropertyName() {
        return Optional.ofNullable(springPropertyKey)
                .map(id -> "spring.security.oauth2.client.registration." + id + ".client-secret")
                .orElse("okta.oauth2.client-secret");
    }
}
