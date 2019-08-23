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
package com.okta.maven.orgcreation.service

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.okta.maven.orgcreation.WireMockSupport
import com.okta.maven.orgcreation.model.OrganizationRequest
import com.okta.maven.orgcreation.model.OrganizationResponse
import org.testng.annotations.Test

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.hamcrest.Matchers.is
import static org.hamcrest.MatcherAssert.assertThat

class DefaultOrganizationCreatorTest implements WireMockSupport {

    @Override
    Collection<StubMapping> wireMockStubMapping() {
        return [
            WireMock.post("/org/create")
                .withRequestBody(WireMock.equalToJson("""
                    {
                      "firstName": "Joe",
                      "lastName": "Coder",
                      "email": "joe.coder@example.com",
                      "organization": "Test co"
                    }
                    """))
                .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .willReturn(aResponse()
                    .withHeader("Content-Type","application/json")
                    .withBody(basicSuccess()))
        ]
    }

    @Test
    void basicSuccessTest() {

        DefaultOktaOrganizationCreator creator = new DefaultOktaOrganizationCreator()
        OrganizationResponse response = creator.createNewOrg(mockUrl(), new OrganizationRequest()
            .setEmail("joe.coder@example.com")
            .setOrganization("Test co")
            .setFirstName("Joe")
            .setLastName("Coder"))

        assertThat response.orgUrl, is("https://okta.example.com")
        assertThat response.apiToken, is("an-api-token-here")
        assertThat response.email, is("joe.coder@example.com")

        // check that the User-Agent was set
        getWireMockServer().verify(postRequestedFor(urlEqualTo("/org/create")).withHeader("User-Agent", WireMock.containing("okta-maven-plugin/")))
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
