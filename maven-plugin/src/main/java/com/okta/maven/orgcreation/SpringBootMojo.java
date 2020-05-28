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
package com.okta.maven.orgcreation;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ConfigFileLocatorService;
import com.okta.commons.lang.ApplicationInfo;
import com.okta.maven.orgcreation.service.DependencyAddService;
import com.okta.maven.orgcreation.service.LatestVersionService;
import com.okta.maven.orgcreation.service.PomUpdateException;
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
import java.util.List;

/**
 * Creates a new Okta OIDC Web Application for use with your Spring Boot application and writes an
 * {@code src/main/application.properties|yml} file with it's configuration.
 * <p>
 * NOTE: You must have an existing Okta account to use this Mojo, use the goals: {@code okta:register} or {@code okta:login}.
 * <p>
 * To create other types of applications on the command line see the <a href="https://github.com/oktadeveloper/okta-maven-plugin">Okta CLI</a>.
 */
@Mojo(name = "spring-boot", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class SpringBootMojo extends BaseAppMojo {

    private static final String GROUP_ID = "com.okta.spring";
    private static final String ARTIFACT_ID = "okta-spring-boot-starter";
    private static final String DEFAULT_VERSION = ApplicationInfo.get().getOrDefault("okta-maven-plugin", "${okta.version}");

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * Spring configuration file, an empty value (default) will instruct the plugin to look for both
     * {@code src/main/resources/application.yml} and {@code src/main/resources/application.properties} files. If
     * neither is found {@code src/main/resources/application.yml} is used.
     */
    @Parameter(property = "applicationConfigFile")
    protected File applicationConfigFile;

    /**
     * Optional Groups claim name, empty by default.
     */
    @Parameter(property = "groupClaimName", defaultValue = "")
    private String groupClaimName;

    @Component
    protected DependencyAddService dependencyAddService;

    @Component
    protected LatestVersionService latestVersionService;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    protected ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    protected List<ArtifactRepository> remoteArtifactRepositories;

    /**
     * The redirect URI used for the OIDC application.
     */
    @Parameter(property = "redirectUri", defaultValue = "http://localhost:8080/login/oauth2/code/okta")
    protected String redirectUri = "http://localhost:8080/login/oauth2/code/okta";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createWebApplication("okta", groupClaimName, redirectUri);

        // add okta-spring-boot-starter to the pom.xml
        if (project != null && project.getFile() != null) {
            updatePomFileWithOktaDependency();
        } else {
            getLog().warn("This project has no pom.xml file, see https://github.com/okta/okta-spring-boot for setup instructions.");
        }
    }

    @Override
    MutablePropertySource getPropertySource() {
        return new ConfigFileLocatorService().findApplicationConfig(baseDir, applicationConfigFile);
    }

    private void updatePomFileWithOktaDependency() throws MojoExecutionException, MojoFailureException {
        if (!hasOktaDependency() && isSpringBoot()) {

            String version;
            try {
                version = latestVersionService.getLatestVersion(GROUP_ID, ARTIFACT_ID, DEFAULT_VERSION, localRepository, remoteArtifactRepositories).toString();
                getLog().debug("latest version: " + version);

                // add dependency to pom and write
                dependencyAddService.addDependencyToPom(GROUP_ID, ARTIFACT_ID, version, project);
            } catch (ArtifactMetadataRetrievalException e) {
                throw new MojoExecutionException("Failed to lookup latest version of '" + GROUP_ID + ":" + ARTIFACT_ID + "', see https://github.com/okta/okta-spring-boot for instructions.", e);
            } catch (PomUpdateException e) {
                logErrorManualWorkAround(DEFAULT_VERSION);
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

    boolean isSpringBoot() {
        return project.getDependencies().stream()
                .anyMatch(dependency -> dependency.getGroupId().contains("org.springframework.boot"));
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