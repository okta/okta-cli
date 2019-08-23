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
package com.okta.maven.orgcreation;

import com.okta.maven.orgcreation.model.OrganizationResponse;
import com.okta.maven.orgcreation.model.OrganizationRequest;
import com.okta.maven.orgcreation.service.ConfigFileUtil;
import com.okta.maven.orgcreation.service.OidcAppCreator;
import com.okta.maven.orgcreation.service.OktaOrganizationCreator;
import com.okta.maven.orgcreation.service.SdkConfigurationService;
import com.okta.maven.orgcreation.spring.MutablePropertySource;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.resource.ExtensibleResource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mojo(name = "init", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class OrgCreationMojo extends AbstractMojo {

    @Parameter(property = "email")
    private String email;

    @Parameter(property = "firstName")
    private String firstName;

    @Parameter(property = "lastName")
    private String lastName;

    @Parameter(property = "company")
    private String company;

    @Parameter(property = "applicationConfigFile")
    private File applicationConfigFile;

    @Parameter(property = "apiUrl", defaultValue = "https://obscure-atoll-66316.herokuapp.com")
    private String apiBaseUrl;

    @Parameter(property = "oidcAppName", defaultValue = "${project.name}")
    private String oidcAppName;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    @Parameter(defaultValue = "${user.home}/.okta/okta.yaml", readonly = true)
    private File oktaPropsFile;

    @Component
    private Prompter prompter;

    @Component
    private OktaOrganizationCreator organizationCreator;

    @Component
    private OidcAppCreator oidcAppCreator;

    @Component
    private SdkConfigurationService sdkConfigurationService;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // check if okta client config exists?
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();

        try {

            String orgUrl;

            if (StringUtils.isEmpty(clientConfiguration.getBaseUrl())) {
                getLog().info("Current OrgUrl is empty, creating new org...");

                OrganizationResponse newOrg = organizationCreator.createNewOrg(apiBaseUrl, organizationRequest());
                orgUrl = newOrg.getOrgUrl();

                getLog().info("OrgUrl: "+ orgUrl);
                getLog().info("Check your email address to verify your account.\n");

                getLog().info("Writing Okta SDK config to: "+ oktaPropsFile.getAbsolutePath());
                // write ~/.okta/okta.yaml
                sdkConfigurationService.writeOktaYaml(orgUrl, newOrg.getApiToken(), oktaPropsFile);

            } else {
                orgUrl = clientConfiguration.getBaseUrl();
                getLog().info("Current OrgUrl: " + clientConfiguration.getBaseUrl());
            }

            // Create new Application

            MutablePropertySource propertySource = ConfigFileUtil.findSpringApplicationConfig(baseDir, applicationConfigFile);

            String clientId = propertySource.getProperty("okta.oauth2.client-id");

            if (StringUtils.isEmpty(clientId)) {

                // create ODIC application
                Client client = Clients.builder().build();

                ExtensibleResource clientCredsResponse = oidcAppCreator.createOidcApp(client, oidcAppName);

                Map<String, String> newProps = new HashMap<>();
                newProps.put("okta.oauth2.issuer", orgUrl + "/oauth2/default");
                newProps.put("okta.oauth2.client-id", clientCredsResponse.getString("client_id"));
                newProps.put("okta.oauth2.client-secret", clientCredsResponse.getString("client_secret"));

                propertySource.addProperties(newProps);

                getLog().info("Created OIDC application, client-id: " + clientCredsResponse.getString("client_id"));
            } else {
                getLog().info("Existing OIDC application detected for clientId: "+ clientId + ", skipping new application creation");
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write configuration file: " + e.getMessage(), e);
        }
    }

    private OrganizationRequest organizationRequest() throws MojoExecutionException {
        return new OrganizationRequest()
            .setFirstName(promptIfNull(firstName, "firstName", "First name"))
            .setLastName(promptIfNull(lastName, "lastName", "Last name"))
            .setEmail(promptIfNull(email, "email", "Email Address"))
            .setOrganization(promptIfNull(company, "company", "Company"));
    }

    private String promptIfNull(String currentValue, String keyName, String promptText) throws MojoExecutionException {

        String value = currentValue;

        if (StringUtils.isEmpty(value)) {
            if (settings.isInteractiveMode()) {
                try {
                    value = prompter.prompt(promptText);
                }
                catch (PrompterException e) {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
            } else {
                throw new MojoExecutionException( "You must specify the '" + keyName + "' property either on the command line " +
                        "-D" + keyName + "=... or run in interactive mode" );
            }
        }
        return value;
    }
}
