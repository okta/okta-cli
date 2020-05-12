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
package com.okta.cli.test

import okhttp3.mockwebserver.MockWebServer
import org.testng.annotations.Test

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class JHipsterIT implements MockWebSupport, CreateAppSupport {

    @Test
    void happyPath() {

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {

            String orgBaseUrl = mockWebServer.url("").toString().replaceFirst("/\$", "")

            mockWebServer.enqueue(jsonRequest("""{ "orgUrl": "${orgBaseUrl}", "email": "test-email@example.com", "apiToken": "fake-test-token" }"""))

            // create new app
            mockWebServer.enqueue(jsonRequest("[]")) // GET /api/v1/apps?q=<my-app-name>
            mockWebServer.enqueue(jsonRequest(application())) // POST /api/v1/apps

            // get Everyone group id
            mockWebServer.enqueue(jsonRequest("[${everyoneGroup()}]")) // GET /api/v1/groups?q=everyone
            mockWebServer.enqueue(jsonRequest("{}")) // PUT /api/v1/apps/test-app-id/groups/every1-id

            // assign app to group
            mockWebServer.enqueue(jsonRequest(oidcAppCredentials())) // GET /api/v1/internal/apps/test-app-id/settings/clientcreds

            // TODO: this can probably be ignored here, and make sure we have UT to cover it
            // a request to get claims
            mockWebServer.enqueue(jsonRequest("{}"))
            // a request to add the `groups` claim
            mockWebServer.enqueue(jsonRequest("{}"))

            // TODO need to figure out how to setup the trust store in the native binary for testing?
            def result = new CommandRunner(mockWebServer.url("/").toString()).runCommandWithInput(registrationInputs, "-Dokta.testing.disableHttpsCheck=true", "jhipster")
            assertThat result, resultMatches(0, allOf(containsString("Check your email address to verify your account"),
                                                               containsString("OrgUrl: ${orgBaseUrl}")),
                                                         null) // TODO warnings are printed because this test includes disableHttpsCheck

            // verify collected info was sent
            verifyOrgCreateRequest(mockWebServer.takeRequest())

            // validate the user-agent is correct
            verifyUserAgent(mockWebServer.takeRequest())

            verifyJsonRequestBody(mockWebServer.takeRequest(), containsString("http://localhost:8080/callback"),
                                                               containsString("http://localhost:8080/login/oauth2/code/oidc"))

            // verify okta.yaml was configured with token
            verifyOktaConfig(result, orgBaseUrl)

            // verify .okta.env
            File envFile = new File(result.workDir, ".okta.env")
            assertThat "Expected ${envFile} to exist", envFile.exists()
            assertThat envFile.text, equalTo(
"""export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_SECRET="test-client-secret"
export SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI="${orgBaseUrl}/oauth2/default"
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_OIDC_CLIENT_ID="test-client-id"
""".toString())
        }
    }
}