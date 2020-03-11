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
import com.okta.maven.orgcreation.progressbar.ProgressBar;
import com.okta.maven.orgcreation.service.AuthorizationServerConfigureService;
import com.okta.maven.orgcreation.service.ConfigFileUtil;
import com.okta.maven.orgcreation.service.DependencyAddService;
import com.okta.maven.orgcreation.service.LatestVersionService;
import com.okta.maven.orgcreation.service.OidcAppCreator;
import com.okta.maven.orgcreation.service.OktaOrganizationCreator;
import com.okta.maven.orgcreation.service.PomUpdateException;
import com.okta.maven.orgcreation.service.SdkConfigurationService;
import com.okta.maven.orgcreation.spring.MutablePropertySource;
import com.okta.maven.orgcreation.support.SuppressFBWarnings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.impl.config.ClientConfiguration;
import com.okta.sdk.resource.ExtensibleResource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a free Okta Developer account and registers / configures a new OIDC application. To signup for an account
 * without using this plugin visit  <a href="https://developer.okta.com/signup">developer.okta.com</a>.
 * <p>
 * If you have an existing Okta account, you can configure this plugin to use it by configuring a
 * {@code ~/.okta/okta.yaml} configuration (or equivalent, see the <a href="https://github.com/okta/okta-sdk-java#configuration-reference">Okta SDK configuration reference</a> for more info).
 * <p>
 * TL;DR: If you have an exiting account create a <code>~/.okta/okta.yaml</code> with your URL and API token:
 * <pre><code>
 * okta:
 *   client:
 *     orgUrl: https://{yourOktaDomain}
 *     token: {yourApiToken}
 * </code></pre>
 *
 */
@Mojo(name = "setup", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class SetupMojo extends AbstractMojo {

    private static final String GROUP_ID = "com.okta.spring";
    private static final String ARTIFACT_ID = "okta-spring-boot-starter";
    private static final String DEFAULT_VERSION = "1.3.0";

    /**
     * Email used when registering a new Okta account.
     */
    @Parameter(property = "email")
    private String email;

    /**
     * First name used when registering a new Okta account.
     */
    @Parameter(property = "firstName")
    private String firstName;

    /**
     * Last name used when registering a new Okta account.
     */
    @Parameter(property = "lastName")
    private String lastName;

    /**
     * Company / organization used when registering a new Okta account.
     */
    @Parameter(property = "company")
    private String company;

    /**
     * Spring configuration file, an empty value (default) will instruct the plugin to look for both
     * {@code src/main/resources/application.yml} and {@code src/main/resources/application.properties} files.  If
     * neither is found {@code src/main/resources/application.yml} is used.
     */
    @Parameter(property = "applicationConfigFile")
    private File applicationConfigFile;

    @Parameter(property = "groupClaimName", defaultValue = "groups")
    private String groupClaimName;

    @Parameter(property = "createGroupClaim")
    private boolean createGroupClaim;

    /**
     * The base URL of the service used to create a new Okta account.
     * This value is NOT exposed as a plugin parameter, but CAN be set using the system property {@code okta.maven.apiBaseUrl}.
     */
    private String apiBaseUrl = "https://start.okta.dev/";

    /**
     * The id of the authorization server.
     */
    @Parameter(property = "authorizationServerId", defaultValue = "default")
    private String authorizationServerId = "default";

    /**
     * The Name / Label of the new OIDC application that will be created.  If an application with the same name already
     * exists, that application will be used.
     */
    @Parameter(property = "oidcAppName", defaultValue = "${project.name}")
    private String oidcAppName;

    @Parameter(property = "useOktaPropertyNames", defaultValue = "true")
    private boolean useOktaPropertyNames = true;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    @Parameter(defaultValue = "${user.home}/.okta/okta.yaml", readonly = true)
    private File oktaPropsFile;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    protected ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remoteArtifactRepositories;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * Set {@code demo} to {@code true} to force the prompt to collect user info.
     * This makes for a better demo without needing to create a new Okta Organization each time.
     * <p><b>NOTE:</b> Most users will ignore this property.
     */
    @Parameter(property = "okta.demo", defaultValue = "false")
    protected boolean demo = false;

    @Component
    private Prompter prompter;

    @Component
    private OktaOrganizationCreator organizationCreator;

    @Component
    private OidcAppCreator oidcAppCreator;

    @Component
    private SdkConfigurationService sdkConfigurationService;

    @Component
    protected DependencyAddService dependencyAddService;

    @Component
    protected LatestVersionService latestVersionService;

    @Component
    protected AuthorizationServerConfigureService authorizationServerConfigureService;

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive on Java 11")
    public void execute() throws MojoExecutionException, MojoFailureException {

        // check if okta client config exists?
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();

        try {
            String orgUrl;
            try (ProgressBar progressBar = ProgressBar.create(settings.isInteractiveMode())) {
                if (StringUtils.isEmpty(clientConfiguration.getBaseUrl())) {

                    // prompt for info before progress bar
                    OrganizationRequest request = organizationRequest();

                    progressBar.start("Creating new Okta Organization, this may take a minute:");

                    OrganizationResponse newOrg = organizationCreator.createNewOrg(getApiBaseUrl(), request);
                    orgUrl = newOrg.getOrgUrl();

                    progressBar.info("OrgUrl: " + orgUrl);
                    progressBar.info("Check your email address to verify your account.\n");

                    progressBar.info("Writing Okta SDK config to: " + oktaPropsFile.getAbsolutePath());
                    // write ~/.okta/okta.yaml
                    sdkConfigurationService.writeOktaYaml(orgUrl, newOrg.getApiToken(), oktaPropsFile);

                } else {
                    if (demo) { // always prompt for user info in "demo mode", this info will not be used but it makes for a more realistic demo
                        organizationRequest();
                    }

                    orgUrl = clientConfiguration.getBaseUrl();
                    progressBar.info("Current OrgUrl: " + clientConfiguration.getBaseUrl());
                }
            }

            // Create new Application
            MutablePropertySource propertySource = ConfigFileUtil.findSpringApplicationConfig(baseDir, applicationConfigFile);
            String clientId = propertySource.getProperty(getClientIdPropertyName());

            try (ProgressBar progressBar = ProgressBar.create(settings.isInteractiveMode())) {
                if (StringUtils.isEmpty(clientId)) {

                     progressBar.start("Configuring a new OIDC Application, almost done:");

                     // create ODIC application
                     Client client = Clients.builder().build();

                     ExtensibleResource clientCredsResponse = oidcAppCreator.createOidcApp(client, oidcAppName);

                     Map<String, String> newProps = new HashMap<>();
                     newProps.put(getIssuerUriPropertyName(), orgUrl + "/oauth2/default");
                     newProps.put(getClientIdPropertyName(), clientCredsResponse.getString("client_id"));
                     newProps.put(getClientSecretPropertyName(), clientCredsResponse.getString("client_secret"));

                     propertySource.addProperties(newProps);

                    progressBar.info("Created OIDC application, client-id: " + clientCredsResponse.getString("client_id"));

                    if (createGroupClaim) {
                        progressBar.info("Creating Authorization Server claim '" + groupClaimName + "':");
                        boolean asCreated = authorizationServerConfigureService.createGroupClaim(client, groupClaimName, authorizationServerId);
                        if (!asCreated) {
                            getLog().warn("Could not create an Authorization Server claim with the name of '" + groupClaimName + "', it likely already exists. You can verify this in your Okta Admin console.");
                        }
                    }
                } else {
                    progressBar.info("Existing OIDC application detected for clientId: "+ clientId + ", skipping new application creation\n");
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write configuration file: " + e.getMessage(), e);
        }

        // add okta-spring-boot-starter to the pom.xml
        if (project != null && project.getFile() != null) {
            updatePomFileWithOktaDependency();
        } else {
            getLog().warn("This project has no pom.xml file, see https://github.com/okta/okta-spring-boot for setup instructions.");
        }
    }

    private OrganizationRequest organizationRequest() throws MojoExecutionException {
        return new OrganizationRequest()
            .setFirstName(promptIfNull(firstName, "firstName", "First name"))
            .setLastName(promptIfNull(lastName, "lastName", "Last name"))
            .setEmail(promptIfNull(email, "email", "Email address"))
            .setOrganization(promptIfNull(company, "company", "Company"));
    }

    private String promptIfNull(String currentValue, String keyName, String promptText) throws MojoExecutionException {

        String value = currentValue;

        if (StringUtils.isEmpty(value)) {
            if (settings.isInteractiveMode()) {
                try {
                    value = prompter.prompt(promptText);
                    value = promptIfNull(value, keyName, promptText);
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

    private String getApiBaseUrl() {
        return System.getProperty("okta.maven.apiBaseUrl", apiBaseUrl);
    }

    private void updatePomFileWithOktaDependency() throws MojoExecutionException, MojoFailureException {
        if (!hasOktaDependency()) {

            String version = DEFAULT_VERSION;
            try {
                version = latestVersionService.getLatestVersion(GROUP_ID, ARTIFACT_ID, DEFAULT_VERSION, localRepository, remoteArtifactRepositories).toString();
                getLog().debug("latest version: " + version);

                // add dependency to pom and write
                dependencyAddService.addDependencyToPom(GROUP_ID, ARTIFACT_ID, version, project);
            } catch (ArtifactMetadataRetrievalException e) {
                throw new MojoExecutionException("Failed to lookup latest version of '" + GROUP_ID + ":" + ARTIFACT_ID + "', see https://github.com/okta/okta-spring-boot for instructions.", e);
            } catch (PomUpdateException e) {
                logErrorManualWorkAround(version);
                throw new MojoFailureException("Failed to add dependency to Maven pom.xml, see log or more details.", e);
            }
        } else {
            getLog().info("Dependency: 'com.okta.spring:okta-spring-boot-starter' found in project.");
        }
    }

    boolean hasOktaDependency() {
        return project.getDependencies().stream()
                .filter(dependency -> GROUP_ID.equals(dependency.getGroupId()))
                .anyMatch(dependency -> ARTIFACT_ID.equals(dependency.getArtifactId()));
    }

    private void logErrorManualWorkAround(String latestOktaVersion) {
        getLog().error("The " + ARTIFACT_ID + " could not be automatically added to the pom.xml, add the following XML snippet manually:");
        getLog().error("\n    <dependency>\n" +
                "        <groupId>" + GROUP_ID + "</groupId>\n" +
                "        <artifactId>" + ARTIFACT_ID + "</artifactId>\n" +
                "        <version>" + latestOktaVersion + "</version>\n" +
                "    </dependency>");
    }

    private String getIssuerUriPropertyName() {
        return useOktaPropertyNames
            ? "okta.oauth2.issuer"
            : "spring.security.oauth2.client.provider.oidc.issuer-uri";
    }

    private String getClientIdPropertyName() {
        return useOktaPropertyNames
                ? "okta.oauth2.client-id"
                : "spring.security.oauth2.client.registration.oidc.client-id";
    }

    private String getClientSecretPropertyName() {
        return useOktaPropertyNames
                ? "okta.oauth2.client-secret"
                : "spring.security.oauth2.client.registration.oidc.client-secret";
    }
}
