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

import com.okta.cli.common.model.OrganizationResponse
import com.okta.maven.orgcreation.service.DefaultMavenRegistrationService
import org.codehaus.plexus.components.interactivity.Prompter
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.testng.PowerMockObjectFactory
import org.testng.IObjectFactory
import org.testng.annotations.ObjectFactory
import org.testng.annotations.Test

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@PrepareForTest(RegisterMojo)
class RegisterMojoTest {

    @ObjectFactory
    IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory()
    }

    @Test
    void basicConfigTest() {

        def prompter = mock(Prompter)
        def oktaPropsFile = mock(File)
        def demo = true
        def interactive = true
        def mavenRegistrationService = mock(DefaultMavenRegistrationService)
        def firstName = "joe"
        def lastName = "Coder"
        def email = "joe.coder@example.com"
        def company = "Example Co."
        def orgResponse = new OrganizationResponse()
                .setEmail(email)
                .setOrgUrl("https://org.example.com")
                .setIdentifier("test-id")

        PowerMockito.whenNew(DefaultMavenRegistrationService).withArguments(prompter, oktaPropsFile, demo, interactive).thenReturn(mavenRegistrationService)
        when(mavenRegistrationService.register(firstName, lastName, email, company)).thenReturn(orgResponse)

        RegisterMojo mojo = new RegisterMojo()
        mojo.firstName = firstName
        mojo.lastName = lastName
        mojo.email = email
        mojo.company = company
        mojo.interactiveMode = interactive
        mojo.demo = demo
        mojo.prompter = prompter
        mojo.oktaPropsFile = oktaPropsFile

        mojo.execute()
        verify(mavenRegistrationService).register(firstName, lastName, email, company)
        verify(mavenRegistrationService).verify("test-id", null)
    }
}