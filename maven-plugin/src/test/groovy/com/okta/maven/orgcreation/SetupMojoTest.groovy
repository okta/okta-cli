/*
 * Copyright 2019-Present Okta, Inc.
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
import com.okta.cli.common.model.OrganizationRequest
import com.okta.cli.common.service.SdkConfigurationService
import com.okta.cli.common.service.SetupService
import com.okta.maven.orgcreation.service.*
import com.okta.maven.orgcreation.test.RestoreSystemProperties
import com.okta.sdk.impl.config.ClientConfiguration
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.versioning.ArtifactVersion
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.project.MavenProject
import org.apache.maven.repository.legacy.metadata.ArtifactMetadataRetrievalException
import org.apache.maven.settings.Settings
import org.codehaus.plexus.components.interactivity.Prompter
import org.mockito.ArgumentCaptor
import org.testng.annotations.BeforeClass
import org.testng.annotations.Listeners
import org.testng.annotations.Test

import java.util.function.Supplier

import static com.okta.maven.orgcreation.TestUtil.expectException
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

@Listeners([RestoreSystemProperties])
class SetupMojoTest {

    @BeforeClass
    void clearOktaEnvAndSysProps() {
        System.clearProperty("okta.client.token")
        System.clearProperty("okta.client.orgUrl")
    }

    @Test
    void noPriorConfigTest() {

        File testDir = File.createTempDir()
        File sdkConfigFile = new File(testDir, "home-okta.yaml")

        MutablePropertySource propertySource = mock(MutablePropertySource)
        def clientConfig = mock(ClientConfiguration)
        SetupMojo mojo = buildMojo("noPriorConfigTest", propertySource, clientConfig, testDir, sdkConfigFile)
        when(mojo.project.getFile()).thenReturn(File.createTempFile("noPriorConfigTest-", "-pom.xml"))

        mojo.execute()

        ArgumentCaptor<Supplier<OrganizationRequest>> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier)
        verify(mojo.createSetupService("okta"))
                .configureEnvironment(supplierArgumentCaptor.capture(),
                                      eq(mojo.oktaPropsFile),
                                      eq(propertySource),
                                      eq(mojo.oidcAppName),
                                      eq(null),
                                      eq("default"),
                                      eq(false),
                                      eq(false),
                                      eq("http://localhost:8080/authorization-code/callback"),
                                      eq("http://localhost:8080/login/oauth2/code/okta"))

        assertThat(supplierArgumentCaptor.getValue().get(), equalTo(new OrganizationRequest()
                                                                    .setEmail("jill.coder@example.com")
                                                                    .setFirstName("Jill")
                                                                    .setLastName("Coder")
                                                                    .setOrganization("Test co.")))

        verify(mojo.dependencyAddService).addDependencyToPom("com.okta.spring", "okta-spring-boot-starter", "1.2.3", mojo.project)
    }

    @Test
    void promptUserForInfo() {

        SetupMojo mojo = new SetupMojo()
        Prompter prompter = mock(Prompter)
        Settings settings = mock(Settings)
        when(settings.isInteractiveMode()).thenReturn(true)

        when(prompter.prompt("First name")).thenReturn(null, "Joe")
        when(prompter.prompt("Last name")).thenReturn("", "Coder")
        when(prompter.prompt("Email address")).thenReturn(" ", "joe.coder@example.com")
        when(prompter.prompt("Company")).thenReturn("Test Co.")

        mojo.prompter = prompter
        mojo.settings = settings

        OrganizationRequest request = mojo.organizationRequest()
        assertThat request.firstName, is("Joe")
        assertThat request.lastName, is("Coder")
        assertThat request.email, is("joe.coder@example.com")
        assertThat request.organization, is("Test Co.")
    }

    @Test
    void promptErrorNonInteractive() {

        SetupMojo mojo = new SetupMojo()
        Prompter prompter = mock(Prompter)
        Settings settings = mock(Settings)
        when(settings.isInteractiveMode()).thenReturn(false)
        when(prompter.prompt("First name")).thenReturn(null)

        mojo.prompter = prompter
        mojo.settings = settings

        IllegalArgumentException exception = expectException IllegalArgumentException, { mojo.organizationRequest() }
        assertThat(exception.getMessage(), stringContainsInOrder("'firstName'", "-DfirstName=..."))
    }

    @Test
    void noProjectPomTest() {

        File testDir = File.createTempDir()
        File sdkConfigFile = new File(testDir, "home-okta.yaml")

        MutablePropertySource propertySource = mock(MutablePropertySource)
        def clientConfig = mock(ClientConfiguration)
        SetupMojo mojo = buildMojo("noProjectPomTest", propertySource, clientConfig, testDir, sdkConfigFile)
        when(mojo.project.getFile()).thenReturn(null) // this project has no pom.xml file

        mojo.execute()

        ArgumentCaptor<Supplier<OrganizationRequest>> supplierArgumentCaptor = ArgumentCaptor.forClass(Supplier)
        verify(mojo.createSetupService("okta"))
                .configureEnvironment(supplierArgumentCaptor.capture(),
                                                        eq(mojo.oktaPropsFile),
                                                        eq(propertySource),
                                                        eq(mojo.oidcAppName),
                                                        eq(null),
                                                        eq("default"),
                                                        eq(false),
                                                        eq(false),
                                                        eq("http://localhost:8080/authorization-code/callback"),
                                                        eq("http://localhost:8080/login/oauth2/code/okta"))

        assertThat(supplierArgumentCaptor.getValue().get(), equalTo(new OrganizationRequest()
                .setEmail("jill.coder@example.com")
                .setFirstName("Jill")
                .setLastName("Coder")
                .setOrganization("Test co.")))
    }

    SetupMojo buildMojo(String testName,
                        MutablePropertySource propertySource,
                        ClientConfiguration clientConfig = mock(ClientConfiguration),
                        File testDir = File.createTempDir(),
                        File sdkConfigFile = null,
                        File springConfigFile = null,
                        String version = "1.2.3") {

        SetupService setupService = mock(SetupService)
        def settings = mock(Settings)
        def prompter = mock(Prompter)
        def sdkConfigurationService = mock(SdkConfigurationService)
        def project = mock(MavenProject)
        def dependencyAddService = mock(DependencyAddService)
        def latestVersionService = new LatestVersionService() {
            @Override
            ArtifactVersion getLatestVersion(String groupId, String artifactId, String defaultVersion, ArtifactRepository localRepository, List<ArtifactRepository> remoteArtifactRepositories) throws ArtifactMetadataRetrievalException {
                return new DefaultArtifactVersion(version)
            }
        }

        SetupMojo mojo = new SetupMojo() {
            @Override
            SetupService createSetupService(String springPropertyKey) {
                return setupService
            }

            @Override
            MutablePropertySource getPropertySource() {
                return propertySource
            }
        }
        mojo.email = "jill.coder@example.com"
        mojo.firstName = "Jill"
        mojo.lastName = "Coder"
        mojo.company = "Test co."
        mojo.applicationConfigFile = springConfigFile
        mojo.baseDir = testDir
        mojo.settings = settings
        mojo.oidcAppName = testName
        mojo.oktaPropsFile = sdkConfigFile
        mojo.prompter = prompter
        mojo.project = project
        mojo.latestVersionService = latestVersionService
        mojo.dependencyAddService = dependencyAddService

        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)

        return mojo
    }

    @Test
    void batchModeMissingProperty() {

        File testDir = File.createTempDir()
        File sdkConfigFile = new File(testDir, "home-okta.yaml")

        def clientConfig = mock(ClientConfiguration)
        SetupMojo mojo = buildMojo("batchModeMissingProperty", null, clientConfig, testDir, sdkConfigFile)
        mojo.firstName = null // this will trigger a failure because we cannot prompt the user in batch mode

        when(mojo.settings.isInteractiveMode()).thenReturn(false)

        IllegalArgumentException exception = expectException IllegalArgumentException, { mojo.organizationRequest() }
        assertThat exception.getMessage(), allOf(containsString("'firstName'"),
                                                 containsString("-DfirstName="))
    }
}
