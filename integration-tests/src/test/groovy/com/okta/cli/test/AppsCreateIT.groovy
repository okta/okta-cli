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

import groovy.json.JsonSlurper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.testng.annotations.Test

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class AppsCreateIT implements MockWebSupport, CreateAppSupport {

    @Test
    void createSpaApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + mockWebServer.url("/") + '/oauth2/test-as" }]'),
                                        // GET /api/v1/apps?q=integration-tests
                                        jsonRequest('[]'),
                                        // POST /api/v1/apps
                                        jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                                        // GET /api/v1/groups?q=everyone
                                        jsonRequest("[${everyoneGroup()}]"),
                                        // PUT /api/v1/apps/test-app-id/groups/every1-id
                                        jsonRequest('{}'),
                                        //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                                        jsonRequest('{ "client_id": "test-id" }')]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                "2", // type of app choice "spa"
                "",  // default of "test-project"
                "",  // default callback "http://localhost:8080/callback"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input,"--color=never", "apps", "create")


            assertThat result, resultMatches(0, allOf(
                                                            containsString("Okta application configuration:"),
                                                            containsString("okta.oauth2.client-id: test-id"),
                                                            containsString("okta.oauth2.issuer: ${mockWebServer.url("/")}/oauth2/test-as"),
                                                            not(containsString("okta.oauth2.client-secret"))),
                                        null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), "http://localhost:8080/callback")
        }
    }

    @Test
    void createNativeApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + mockWebServer.url("/") + '/oauth2/test-as" }]'),
                                        // GET /api/v1/apps?q=integration-tests
                                        jsonRequest('[]'),
                                        // POST /api/v1/apps
                                        jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                                        // GET /api/v1/groups?q=everyone
                                        jsonRequest("[${everyoneGroup()}]"),
                                        // PUT /api/v1/apps/test-app-id/groups/every1-id
                                        jsonRequest('{}'),
                                        //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                                        jsonRequest('{ "client_id": "test-id" }')]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "3", //  "native" type of app choice
                    "",  // default of "test-project"
                    "",  // default callback "localhost:/callback"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input,"--color=never", "apps", "create")


            assertThat result, resultMatches(0, allOf(
                                                            containsString("Okta application configuration:"),
                                                            containsString("okta.oauth2.client-id: test-id"),
                                                            containsString("okta.oauth2.issuer: ${mockWebServer.url("/")}/oauth2/test-as"),
                                                            not(containsString("okta.oauth2.client-secret"))),
                                                        null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), "localhost:/callback")
        }
    }

    @Test
    void createWebApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + mockWebServer.url("/") + '/oauth2/test-as" }]'),
                                        // GET /api/v1/apps?q=integration-tests
                                        jsonRequest('[]'),
                                        // POST /api/v1/apps
                                        jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                                        // GET /api/v1/groups?q=everyone
                                        jsonRequest("[${everyoneGroup()}]"),
                                        // PUT /api/v1/apps/test-app-id/groups/every1-id
                                        jsonRequest('{}'),
                                        //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                                        jsonRequest('{ "client_id": "test-id", "client_secret": "test-secret" }')]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "", //  default "web" type of app choice
                    "", // generic OIDC app
                    "", // default of "test-project"
                    "", // default callback "http://localhost:8080/callback"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input,"--color=never", "apps", "create")

            assertThat result, resultMatches(0, allOf(
                                                            containsString("Created OIDC application, client-id: test-id"),
                                                            containsString("Okta application configuration has been written to"),
                                                            containsString(".okta.env")),
                                                        null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), "http://localhost:8080/callback")
        }
    }

    @Test
    void createServiceApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + mockWebServer.url("/") + '/oauth2/test-as" }]'),
                                        // GET /api/v1/apps?q=integration-tests
                                        jsonRequest('[]'),
                                        // POST /api/v1/apps
                                        jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                                        // GET /api/v1/groups?q=everyone
                                        jsonRequest("[${everyoneGroup()}]"),
                                        // PUT /api/v1/apps/test-app-id/groups/every1-id
                                        jsonRequest('{}'),
                                        //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                                        jsonRequest('{ "client_id": "test-id", "client_secret": "test-secret" }')]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "4", //  "native" type of app choice
                    "",  // default of "test-project"
                    "",  // default callback "localhost:/callback"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input,"--verbose", "--color=never", "apps", "create")

            assertThat result, resultMatches(0, allOf(
                                                            containsString("Created OIDC application, client-id: test-id"),
                                                            containsString("Okta application configuration has been written to"),
                                                            containsString(".okta.env")),
                                                        null)
        }
    }

    @Test
    void quickTemplateSpringWeb() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + mockWebServer.url("/") + '/oauth2/test-as" }]'),
                                        // GET /api/v1/apps?q=integration-tests
                                        jsonRequest('[]'),
                                        // POST /api/v1/apps
                                        jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                                        // GET /api/v1/groups?q=everyone
                                        jsonRequest("[${everyoneGroup()}]"),
                                        // PUT /api/v1/apps/test-app-id/groups/every1-id
                                        jsonRequest('{}'),
                                        //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                                        jsonRequest('{ "client_id": "test-id", "client_secret": "test-secret" }')]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "", //  default "web" type of app choice
                    "", // generic OIDC app
                    "", // default of "test-project"
                    "", // default callback "http://localhost:8080/login/oauth2/code/okta"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "create", "spring-boot")

            assertThat result, resultMatches(0, allOf(
                                                            containsString("Created OIDC application, client-id: test-id"),
                                                            containsString("Okta application configuration has been written to"),
                                                            containsString("application.properties")),
                                                        null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), "http://localhost:8080/login/oauth2/code/okta")
        }
    }

    private static void verifyRedirectUri(RecordedRequest request, String... expectedRedirectUris) {
        assertThat request.method, equalTo("POST")
        def body = new JsonSlurper().parse(request.getBody().inputStream())
        String[] redirectUris = body.settings.oauthClient.redirect_uris
        assertThat redirectUris, equalTo(expectedRedirectUris)
    }
}
