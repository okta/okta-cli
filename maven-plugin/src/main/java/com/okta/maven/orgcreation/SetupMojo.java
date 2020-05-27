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

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ClientConfigurationException;
import com.okta.cli.common.service.ConfigFileLocatorService;
import com.okta.cli.common.service.SetupService;
import com.okta.maven.orgcreation.service.DependencyAddService;
import com.okta.maven.orgcreation.service.LatestVersionService;
import com.okta.maven.orgcreation.service.PomUpdateException;
import com.okta.maven.orgcreation.support.SuppressFBWarnings;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
public class SetupMojo extends BaseSetupMojo {

    private static final String GROUP_ID = "com.okta.spring";
    private static final String ARTIFACT_ID = "okta-spring-boot-starter";
    private static final String DEFAULT_VERSION = "1.3.0";

    @Parameter(property = "groupClaimName", defaultValue = "groups")
    private String groupClaimName;

    @Parameter(property = "createGroupClaim")
    private boolean createGroupClaim;

    @Parameter(property = "useOktaPropertyNames", defaultValue = "true")
    private boolean useOktaPropertyNames = true;

    @Parameter(property = "springProviderId", defaultValue = "okta")
    private String springProviderId = "okta";

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    protected ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remoteArtifactRepositories;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * Spring configuration file, an empty value (default) will instruct the plugin to look for both
     * {@code src/main/resources/application.yml} and {@code src/main/resources/application.properties} files.  If
     * neither is found {@code src/main/resources/application.yml} is used.
     */
    @Parameter(property = "applicationConfigFile")
    protected File applicationConfigFile;

    @Component
    protected DependencyAddService dependencyAddService;

    @Component
    protected LatestVersionService latestVersionService;

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive on Java 11")
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            String groupClaim = createGroupClaim ? groupClaimName : null;
            String springPropertyKey = useOktaPropertyNames ? null : springProviderId;
            SetupService setupService = createSetupService(springPropertyKey);

            setupService.configureEnvironment(this::organizationRequest,
                                              oktaPropsFile,
                                              getPropertySource(),
                                              oidcAppName,
                                              groupClaim,
                                              null,
                                              authorizationServerId,
                                              demo,
                                              settings.isInteractiveMode(),
                                              springBasedRedirectUris(springProviderId));

        } catch (IOException | ClientConfigurationException e) {
            throw new MojoExecutionException("Failed to setup environment", e);
        }

        // add okta-spring-boot-starter to the pom.xml
        if (project != null && project.getFile() != null) {
            updatePomFileWithOktaDependency();
        } else {
            getLog().warn("This project has no pom.xml file, see https://github.com/okta/okta-spring-boot for setup instructions.");
        }
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

    @Override
    MutablePropertySource getPropertySource() {
        return new ConfigFileLocatorService().findApplicationConfig(baseDir, applicationConfigFile);
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
}
