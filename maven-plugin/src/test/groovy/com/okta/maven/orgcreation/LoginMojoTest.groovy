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

import com.okta.cli.common.service.SdkConfigurationService
import com.okta.sdk.impl.config.ClientConfiguration
import org.codehaus.plexus.components.interactivity.Prompter
import org.testng.annotations.Test

import static com.okta.maven.orgcreation.TestUtil.expectException
import static org.mockito.Mockito.*

class LoginMojoTest {

    @Test
    void promptForBoth() {

        def prompter = mock(Prompter)
        def sdkConfigurationService = mock(SdkConfigurationService)
        def clientConfig = mock(ClientConfiguration)

        when(prompter.prompt("Okta Org URL")).thenReturn("https://test.example.com")
        when(prompter.prompt("Okta API token")).thenReturn("private-token")
        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)

        LoginMojo mojo = new LoginMojo()
        mojo.apiToken = null
        mojo.orgUrl = null
        mojo.prompter = prompter
        mojo.oktaPropsFile = mock(File)
        mojo.interactiveMode = true
        mojo.sdkConfigurationService = sdkConfigurationService
        mojo.out = mock(PrintStream)

        mojo.execute()

        verify(prompter).prompt("Okta Org URL")
        verify(prompter).prompt("Okta API token")
        verifyNoMoreInteractions(prompter)
        verify(sdkConfigurationService).writeOktaYaml("https://test.example.com", "private-token", mojo.oktaPropsFile)
    }

    @Test
    void invalidBaseUrl() {

        def prompter = mock(Prompter)
        def sdkConfigurationService = mock(SdkConfigurationService)
        def clientConfig = mock(ClientConfiguration)

        when(prompter.prompt("Okta Org URL")).thenReturn("http://test.example.com") // non https url
        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)

        LoginMojo mojo = new LoginMojo()
        mojo.apiToken = null
        mojo.orgUrl = null
        mojo.prompter = prompter
        mojo.oktaPropsFile = mock(File)
        mojo.interactiveMode = true
        mojo.sdkConfigurationService = sdkConfigurationService
        mojo.out = mock(PrintStream)

        expectException(IllegalArgumentException) { mojo.execute() }

        verify(prompter).prompt("Okta Org URL")
        verifyNoMoreInteractions(prompter)
    }

    @Test
    void promptDisabled() {

        def prompter = mock(Prompter)
        def sdkConfigurationService = mock(SdkConfigurationService)
        def clientConfig = mock(ClientConfiguration)
        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)

        LoginMojo mojo = new LoginMojo()
        mojo.apiToken = null
        mojo.orgUrl = "https://test.example.com"
        mojo.prompter = prompter
        mojo.oktaPropsFile = mock(File)
        mojo.interactiveMode = false
        mojo.sdkConfigurationService = sdkConfigurationService
        mojo.out = mock(PrintStream)

        expectException(IllegalArgumentException) { mojo.execute() }
        verifyNoMoreInteractions(prompter)
    }

    @Test
    void promptDisabledConfigFromSdk() {
        def prompter = mock(Prompter)
        def sdkConfigurationService = mock(SdkConfigurationService)
        def clientConfig = mock(ClientConfiguration)

        when(sdkConfigurationService.loadUnvalidatedConfiguration()).thenReturn(clientConfig)
        when(clientConfig.getBaseUrl()).thenReturn("https://test.example.com")
        when(clientConfig.getApiToken()).thenReturn("private-token")

        LoginMojo mojo = new LoginMojo()
        mojo.apiToken = null
        mojo.orgUrl = null
        mojo.prompter = prompter
        mojo.oktaPropsFile = mock(File)
        mojo.interactiveMode = true
        mojo.sdkConfigurationService = sdkConfigurationService
        mojo.out = mock(PrintStream)

        mojo.execute()
        verify(sdkConfigurationService).writeOktaYaml("https://test.example.com", "private-token", mojo.oktaPropsFile)
        verifyNoMoreInteractions(prompter)
    }
}
