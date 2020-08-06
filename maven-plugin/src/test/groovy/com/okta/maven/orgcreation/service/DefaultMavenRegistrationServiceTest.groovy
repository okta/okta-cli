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
package com.okta.maven.orgcreation.service

import com.okta.cli.common.service.DefaultSetupService
import org.codehaus.plexus.components.interactivity.Prompter
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.testng.PowerMockObjectFactory
import org.testng.IObjectFactory
import org.testng.annotations.ObjectFactory
import org.testng.annotations.Test

import static com.okta.maven.orgcreation.TestUtil.expectException
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@PrepareForTest(DefaultMavenRegistrationService)
class DefaultMavenRegistrationServiceTest {

    @ObjectFactory
    IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory()
    }

    @Test
    void register() {
        Prompter prompter = mock(Prompter)
        File propsFile = mock(File)
        DefaultSetupService setupService = mock(DefaultSetupService)
        PowerMockito.whenNew(DefaultSetupService).withArguments(null).thenReturn(setupService)

        def registrationService = spy new DefaultMavenRegistrationService(prompter, propsFile, false, false)
        registrationService.register("first-name", "last-name", "email@example.com", "Example Co.")

        verify(setupService).createOktaOrg(any(), eq(propsFile), eq(false), eq(false))
    }

    @Test
    void verifyCode() {
        Prompter prompter = mock(Prompter)
        File propsFile = mock(File)
        DefaultSetupService setupService = mock(DefaultSetupService)
        PowerMockito.whenNew(DefaultSetupService).withArguments(null).thenReturn(setupService)

        def registrationService = spy new DefaultMavenRegistrationService(prompter, propsFile, false, false)
        registrationService.verify("test-id", null)

        verify(setupService).verifyOktaOrg(eq("test-id"), any(), eq(propsFile))
    }


    @Test
    void promptNeededNonInteractive() {
        Prompter prompter = mock(Prompter)
        File propsFile = mock(File)

        def registrationService = new DefaultMavenRegistrationService(prompter, propsFile, false, false)
        def exception = expectException(IllegalArgumentException, { registrationService.organizationRequest(null, null, null, null) })
        assertThat exception.message, containsString("-DfirstName")

        exception = expectException IllegalArgumentException, { registrationService.organizationRequest("first-name", null, null, null) }
        assertThat exception.message, containsString("-DlastName")

        exception = expectException IllegalArgumentException, { registrationService.organizationRequest("first-name", "last-name", null, null) }
        assertThat exception.message, containsString("-Demail")

        exception = expectException IllegalArgumentException, { registrationService.organizationRequest("first-name", "last-name", "email@example.com", null) }
        assertThat exception.message, containsString("-Dcompany")
    }

    @Test
    void promptForCode() {
        Prompter prompter = mock(Prompter)
        File propsFile = mock(File)

        when(prompter.prompt(any(String))).thenReturn("totp-code-string")

        assertThat new DefaultMavenRegistrationService(prompter, propsFile, false, true).codePrompt(null), equalTo("totp-code-string")
    }

    @Test
    void promptForCode_nonInteractive() {
        Prompter prompter = mock(Prompter)
        File propsFile = mock(File)

        expectException IllegalArgumentException, { new DefaultMavenRegistrationService(prompter, propsFile, false, false).codePrompt(null) }
    }
}
