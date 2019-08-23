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

import com.okta.maven.orgcreation.model.OrganizationRequest
import com.okta.maven.orgcreation.model.OrganizationResponse
import com.okta.maven.orgcreation.service.OidcAppCreator
import com.okta.maven.orgcreation.service.DefaultOktaOrganizationCreator
import com.okta.maven.orgcreation.service.SdkConfigurationService
import com.okta.sdk.client.Client
import com.okta.sdk.impl.config.ClientConfiguration
import com.okta.sdk.resource.ExtensibleResource
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.settings.Settings
import org.codehaus.plexus.components.interactivity.Prompter
import org.testng.annotations.Test

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.io.FileMatchers.anExistingFile
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class SetupMojoTest {

    @Test
    void noPriorConfigTest() {

        File testDir = File.createTempDir()
        File sdkConfigFile = new File(testDir, "home-okta.yaml")

        def clientConfig = mock(ClientConfiguration)
        def oidcCredentials = mock(ExtensibleResource)
        SetupMojo mojo = buildMojo("noPriorConfigTest", clientConfig, testDir, sdkConfigFile)

        def orgRequest = new OrganizationRequest()
            .setFirstName("Jill")
            .setLastName("Coder")
            .setEmail("jill.coder@example.com")
            .setOrganization("Test co.")

        def orgResponse = new OrganizationResponse()
            .setEmail("jill.coder@exaple.com")
            .setApiToken("an-api-token")
            .setOrgUrl("http://shinny-and-new.example.com")

        when(mojo.sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)
        when(mojo.organizationCreator.createNewOrg("http://foo.example.com/api", orgRequest)).thenReturn(orgResponse)
        when(mojo.oidcAppCreator.createOidcApp(any(Client), eq("noPriorConfigTest"))).thenReturn(oidcCredentials)
        when(oidcCredentials.getString("client_id")).thenReturn("noPriorConfigTest-client-id")
        when(oidcCredentials.getString("client_secret")).thenReturn("noPriorConfigTest-client-secret")

        mojo.execute()

        verify(mojo.organizationCreator).createNewOrg("http://foo.example.com/api", orgRequest)
        verify(mojo.sdkConfigurationService).writeOktaYaml("http://shinny-and-new.example.com", "an-api-token", sdkConfigFile)
        verify(mojo.oidcAppCreator).createOidcApp(any(Client), eq("noPriorConfigTest"))

        File springConfig = new File(testDir, "src/main/resources/application.yml")
        assertThat springConfig, anExistingFile()
        assertThat TestUtil.readYamlFromFile(springConfig), is([
                okta: [
                    oauth2: [
                        issuer: "http://shinny-and-new.example.com/oauth2/default",
                        "client-id": "noPriorConfigTest-client-id",
                        "client-secret": "noPriorConfigTest-client-secret"]]])
    }

    @Test
    void sdkSetupButNoApp() {

        File testDir = File.createTempDir()
        File sdkConfigFile = new File(testDir, "home-okta.yaml")
        TestUtil.writeYamlToTempFile([
                okta: [
                    client: [
                        orgUrl: "http://shinny-and-new.example.com",
                        token: "an-api-token"
                ]]], sdkConfigFile)

        def oidcCredentials = mock(ExtensibleResource)
        def clientConfig = mock(ClientConfiguration)
        SetupMojo mojo = buildMojo("sdkSetupButNoApp", clientConfig, testDir, sdkConfigFile)

        when(clientConfig.getBaseUrl()).thenReturn( "http://shinny-and-new.example.com")
        when(mojo.sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)
        when(mojo.oidcAppCreator.createOidcApp(any(Client), eq("sdkSetupButNoApp"))).thenReturn(oidcCredentials)
        when(oidcCredentials.getString("client_id")).thenReturn("sdkSetupButNoApp-client-id")
        when(oidcCredentials.getString("client_secret")).thenReturn("sdkSetupButNoApp-client-secret")

        mojo.execute()

        verify(mojo.organizationCreator, never()).createNewOrg(eq("http://foo.example.com/api"), any(OrganizationRequest))
        verify(mojo.oidcAppCreator).createOidcApp(any(Client), eq("sdkSetupButNoApp"))

        File springConfig = new File(testDir, "src/main/resources/application.yml")
        assertThat springConfig, anExistingFile()
        assertThat TestUtil.readYamlFromFile(springConfig), is([
                okta: [
                    oauth2: [
                        issuer: "http://shinny-and-new.example.com/oauth2/default",
                        "client-id": "sdkSetupButNoApp-client-id",
                        "client-secret": "sdkSetupButNoApp-client-secret"]]])
    }

    @Test
    void sdkConfigAndSpringConfigExists() {

        File testDir = File.createTempDir()
        File sdkConfigFile = new File(testDir, "home-okta.yaml")
        TestUtil.writeYamlToTempFile([
                okta: [
                    client: [
                        orgUrl: "http://shinny-and-new.example.com",
                        token: "an-api-token"
                ]]], sdkConfigFile)

        File springConfigFile = new File(testDir, "application.yml")
        TestUtil.writeYamlToTempFile([
                okta: [
                    oauth2: [
                        issuer: "http://shinny-and-new.example.com/issuer",
                        clientId: "sdkConfigAndSpringConfigExists-client-id"
                ]]], springConfigFile)

        def clientConfig = mock(ClientConfiguration)
        SetupMojo mojo = buildMojo("sdkConfigAndSpringConfigExists", clientConfig, testDir, sdkConfigFile, springConfigFile)

        when(clientConfig.getBaseUrl()).thenReturn( "http://shinny-and-new.example.com")

        mojo.execute()

        verify(mojo.organizationCreator, never()).createNewOrg(eq("http://foo.example.com/api"), any(OrganizationRequest))
        verify(mojo.oidcAppCreator, never()).createOidcApp(any(Client), eq("sdkConfigAndSpringConfigExists"))

    }

    SetupMojo buildMojo(String testName,
                        ClientConfiguration clientConfig = mock(ClientConfiguration),
                        File testDir = File.createTempDir(),
                        File sdkConfigFile = null,
                        File springConfigFile = null) {

        def settings = mock(Settings)
        def prompter = mock(Prompter)
        def organizationCreator = mock(DefaultOktaOrganizationCreator)
        def oidcAppCreator = mock(OidcAppCreator)
        def sdkConfigurationService = mock(SdkConfigurationService)

        SetupMojo mojo = new SetupMojo()
        mojo.email = "jill.coder@example.com"
        mojo.firstName = "Jill"
        mojo.lastName = "Coder"
        mojo.company = "Test co."
        mojo.applicationConfigFile = springConfigFile
        mojo.baseDir = testDir
        mojo.settings = settings
        mojo.oidcAppName = testName
        mojo.apiBaseUrl = "http://foo.example.com/api"
        mojo.oktaPropsFile = sdkConfigFile
        mojo.prompter = prompter
        mojo.organizationCreator = organizationCreator
        mojo.oidcAppCreator = oidcAppCreator
        mojo.sdkConfigurationService = sdkConfigurationService

        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)

        return mojo
    }

    @Test
    void batchModeMissingProperty() {

        File testDir = File.createTempDir()
        File sdkConfigFile = new File(testDir, "home-okta.yaml")

        def clientConfig = mock(ClientConfiguration)
        SetupMojo mojo = buildMojo("batchModeMissingProperty", clientConfig, testDir, sdkConfigFile)
        mojo.firstName = null // this will trigger a failure because we cannot prompt the user in batch mode

        when(mojo.settings.isInteractiveMode()).thenReturn(false)
        when(mojo.sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)

        MojoExecutionException exception = TestUtil.expectException MojoExecutionException, { mojo.execute() }
        assertThat exception.getMessage(), allOf(containsString("'firstName'"),
                                                 containsString("-DfirstName="))
    }
}
