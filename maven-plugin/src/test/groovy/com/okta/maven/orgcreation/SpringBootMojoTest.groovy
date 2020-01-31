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
package com.okta.maven.orgcreation

import com.okta.cli.common.config.MutablePropertySource
import com.okta.cli.common.service.SdkConfigurationService
import com.okta.cli.common.service.SetupService
import com.okta.maven.orgcreation.service.DependencyAddService
import com.okta.maven.orgcreation.service.LatestVersionService
import com.okta.sdk.impl.config.ClientConfiguration
import com.okta.sdk.resource.application.OpenIdConnectApplicationType
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException
import org.testng.annotations.Test

import static org.mockito.Mockito.*

class SpringBootMojoTest {

    @Test
    void defaultValuesTest() {

        File projectDir = File.createTempDir()
        SetupService setupService = mock(SetupService)
        MutablePropertySource propertySource = mock(MutablePropertySource)
        SdkConfigurationService sdkConfigurationService = mock(SdkConfigurationService)
        ClientConfiguration clientConfiguration = mock(ClientConfiguration)
        DependencyAddService dependencyAddService = mock(DependencyAddService)
        String version = "1.2.3"
        MavenProject project = mock(MavenProject)
        LatestVersionService latestVersionService = new LatestVersionService() {
            @Override
            ArtifactVersion getLatestVersion(String groupId, String artifactId, String defaultVersion, ArtifactRepository localRepository, List<ArtifactRepository> remoteArtifactRepositories) throws ArtifactMetadataRetrievalException {
                return new DefaultArtifactVersion(version)
            }
        }
        Dependency springBootDep = new Dependency()
        springBootDep.setGroupId("org.springframework.boot")
        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfiguration)
        when(clientConfiguration.getBaseUrl()).thenReturn("https://test.example.com")
        when(clientConfiguration.getApiToken()).thenReturn("test-api-token")
        when(project.dependencies).thenReturn([springBootDep])
        when(project.getFile()).thenReturn(File.createTempFile("pom", ".xml"))

        SpringBootMojo mojo = spy(new SpringBootMojo(){
            @Override
            SetupService createSetupService(String springPropertyKey) {
                return setupService
            }

            @Override
            MutablePropertySource getPropertySource() {
                return propertySource
            }
        })
        mojo.oidcAppName = "test-app-name"
        mojo.baseDir = projectDir
        mojo.sdkConfigurationService = sdkConfigurationService
        mojo.dependencyAddService = dependencyAddService
        mojo.latestVersionService = latestVersionService
        mojo.project = project

        mojo.execute()

        verify(mojo).createSetupService(null)
        verify(setupService).createOidcApplication(propertySource, "test-app-name", "https://test.example.com", null, null, "default", true, OpenIdConnectApplicationType.WEB, "http://localhost:8080/login/oauth2/code/okta")
        verify(mojo.dependencyAddService).addDependencyToPom("com.okta.spring", "okta-spring-boot-starter", "1.2.3", mojo.project)
    }
}
