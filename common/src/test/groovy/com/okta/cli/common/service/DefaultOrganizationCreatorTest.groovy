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
package com.okta.cli.common.service

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.okta.cli.common.RestoreEnvironmentVariables
import com.okta.cli.common.WireMockSupport
import com.okta.cli.common.model.OrganizationRequest
import com.okta.cli.common.model.OrganizationResponse
import org.testng.annotations.Listeners
import org.testng.annotations.Test

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is

@Listeners([RestoreEnvironmentVariables])
class DefaultOrganizationCreatorTest implements WireMockSupport {

    @Override
    Collection<StubMapping> wireMockStubMapping() {
        return [
                post("/create")
                .withRequestBody(equalToJson("""
                    {
                      "firstName": "Joe",
                      "lastName": "Coder",
                      "email": "joe.coder@example.com",
                      "organization": "Test co",
                      "country": "Country"
                    }
                    """))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                    .withHeader("Content-Type","application/json")
                    .withBody(basicSuccess()))
        ]
    }

    @Test
    void basicSuccessTest() {

        RestoreEnvironmentVariables.setEnvironmentVariable("OKTA_CLI_BASE_URL", mockUrl())

        DefaultOktaOrganizationCreator creator = new DefaultOktaOrganizationCreator()
        OrganizationResponse response = creator.createNewOrg(new OrganizationRequest()
            .setEmail("joe.coder@example.com")
            .setOrganization("Test co")
            .setFirstName("Joe")
            .setLastName("Coder")
            .setCountry("Country"))

        assertThat response.orgUrl, is("https://okta.example.com")
        assertThat response.apiToken, is("an-api-token-here")
        assertThat response.email, is("joe.coder@example.com")
    }

    private String basicSuccess() {
        return """
        {
            "email": "joe.coder@example.com",
            "apiToken": "an-api-token-here",
            "orgUrl": "https://okta.example.com"
        }
        """
    }
}
