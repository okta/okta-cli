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
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
                                        // GET /api/v1/apps?q=integration-tests
                                        jsonRequest('[]'),
                                        // POST /api/v1/apps
                                        jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                                        // GET /api/v1/groups?q=everyone
                                        jsonRequest("[${everyoneGroup()}]"),
                                        // PUT /api/v1/apps/test-app-id/groups/every1-id
                                        jsonRequest('{}'),
                                        //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                                        jsonRequest('{ "client_id": "test-id" }'),
                                        // GET /api/v1/trustedOrigins
                                        jsonRequest('[]'),
                                        // POST /api/v1/trustedOrigins
                                        jsonRequest('{}')
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "",  // default of "test-project"
                    "2", // type of app choice "spa"
                    "",  // default callback "http://localhost:8080/callback"
                    "",  // default post logout redirect
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommandWithInput(input,"--color=never", "apps", "create")


            assertThat result, resultMatches(0, allOf(
                                                            containsString("Enter your Redirect URI(s) [http://localhost:8080/callback]:"),
                                                            containsString("Enter your Post Logout Redirect URI(s) [http://localhost:8080/]:"),
                                                            containsString("Okta application configuration:"),
                                                            containsString("Client ID: test-id"),
                                                            containsString("Issuer:    ${url(mockWebServer,"/")}/oauth2/test-as"),
                                                            not(containsString("okta.oauth2.client-secret"))),
                                        null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/authorizationServers")
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps", "q=test-project")
            verifyRedirectUri(mockWebServer.takeRequest(), ["http://localhost:8080/callback"])
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/groups", "q=everyone")
            verify(mockWebServer.takeRequest(), "PUT", "/api/v1/apps/test-app-id/groups/every1-id")
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/internal/apps/test-app-id/settings/clientcreds")
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/trustedOrigins")
            verifyTrustedOrigins(mockWebServer.takeRequest(), "http://localhost:8080/")
        }
    }

    // This test is different from the previous one in that it runs `apps create spa`
    @Test
    void createAppSpa() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                // GET /api/v1/authorizationServers
                jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
                // GET /api/v1/apps?q=integration-tests
                jsonRequest('[]'),
                // POST /api/v1/apps
                jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                // GET /api/v1/groups?q=everyone
                jsonRequest("[${everyoneGroup()}]"),
                // PUT /api/v1/apps/test-app-id/groups/every1-id
                jsonRequest('{}'),
                //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                jsonRequest('{ "client_id": "test-id" }'),
                // GET /api/v1/trustedOrigins
                jsonRequest('[]'),
                // POST /api/v1/trustedOrigins
                jsonRequest('{}')
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "",  // default of "test-project"
                    "",  // default callback "http://localhost:8080/callback"
                    "",  // default post logout redirect
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommandWithInput(input,"--color=never", "apps", "create", "spa")


            assertThat result, resultMatches(0, allOf(
                    containsString("Enter your Redirect URI(s) [http://localhost:8080/callback]:"),
                    containsString("Enter your Post Logout Redirect URI(s) [http://localhost:8080/]:"),
                    containsString("Okta application configuration:"),
                    containsString("Client ID: test-id"),
                    containsString("Issuer:    ${url(mockWebServer,"/")}/oauth2/test-as"),
                    not(containsString("okta.oauth2.client-secret"))),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/authorizationServers")
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps", "q=test-project")
            verifyRedirectUri(mockWebServer.takeRequest(), ["http://localhost:8080/callback"])
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/groups", "q=everyone")
            verify(mockWebServer.takeRequest(), "PUT", "/api/v1/apps/test-app-id/groups/every1-id")
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/internal/apps/test-app-id/settings/clientcreds")
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/trustedOrigins")
            verifyTrustedOrigins(mockWebServer.takeRequest(), "http://localhost:8080/")
        }
    }

    @Test
    void createNativeApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
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
                    "",  // default of "test-project"
                    "3", // "native" type of app choice
                    "",  // default callback "localhost:/callback"
                    "",  // default post logout redirect localhost:/
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommandWithInput(input,"--color=never", "apps", "create")


            assertThat result, resultMatches(0, allOf(
                                                            containsString("Okta application configuration:"),
                                                            containsString("okta.oauth2.client-id: test-id"),
                                                            containsString("Enter your Redirect URI(s) [localhost:/callback]:"),
                                                            containsString("Enter your Post Logout Redirect URI(s) [localhost:/]:"),
                                                            containsString("okta.oauth2.issuer: ${url(mockWebServer,"/")}/oauth2/test-as"),
                                                            not(containsString("okta.oauth2.client-secret"))),
                                                        null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), ["localhost:/callback"])
        }
    }

    // This test is different from the previous one in that it runs `apps create native`
    @Test
    void createAppNative() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                // GET /api/v1/authorizationServers
                jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
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
                    "",  // default of "test-project"
                    "",  // default callback "localhost:/callback"
                    "",  // default post logout redirect localhost:/
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommandWithInput(input,"--color=never", "apps", "create", "native")


            assertThat result, resultMatches(0, allOf(
                    containsString("Okta application configuration:"),
                    containsString("okta.oauth2.client-id: test-id"),
                    containsString("Enter your Redirect URI(s) [localhost:/callback]:"),
                    containsString("Enter your Post Logout Redirect URI(s) [localhost:/]:"),
                    containsString("okta.oauth2.issuer: ${url(mockWebServer,"/")}/oauth2/test-as"),
                    not(containsString("okta.oauth2.client-secret"))),
                    null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), ["localhost:/callback"])
        }
    }

    @Test
    void createAppNativeNotLoggedIn() {

        List<String> input = [
                "",  // default of "test-project"
        ]

        def result = new CommandRunner()
                .runCommandWithInput(input,"--color=never", "apps", "create", "native")

        assertThat result, resultMatches(1, null,
                containsString("Unable to find base URL, run `okta login` and try again"))
    }

    @Test
    void createWebApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
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
                    "", // generic OIDC app
                    "", // default "web" type of app choice
                    "", // default of "test-project"
                    "", // default callback "http://localhost:8080/callback"
                    "", // default post logout redirect http://localhost:8080/
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommandWithInput(input,"--color=never", "apps", "create")

            assertThat result, resultMatches(0, allOf(
                                                            containsString("Created OIDC application, client-id: test-id"),
                                                            containsString("Okta application configuration has been written to"),
                                                            containsString("Enter your Redirect URI(s) [http://localhost:8080/callback]:"),
                                                            containsString("Enter your Post Logout Redirect URI(s) [http://localhost:8080/]:"),
                                                            containsString(".okta.env")),
                                                        null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), ["http://localhost:8080/callback"])
        }
    }

    @Test
    void createWebAppMultipleRedirect() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                // GET /api/v1/authorizationServers
                jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
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
                    "", // default of "test-project"
                    "", //  default "web" type of app choice
                    "", // generic OIDC app
                    "http://localhost:8080/callback, http://localhost:8888/callback", // redirect uris
                    "", // default post logout redirect "http://localhost:8080/, http://localhost:8888/"
                    "", // generic OIDC app
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommandWithInput(input,"--color=never", "apps", "create")

            assertThat result, resultMatches(0, allOf(
                    containsString("Created OIDC application, client-id: test-id"),
                    containsString("Okta application configuration has been written to"),
                    containsString("Enter your Redirect URI(s) [http://localhost:8080/callback]:"),
                    containsString("Enter your Post Logout Redirect URI(s) [http://localhost:8080/, http://localhost:8888/]:"),
                    containsString(".okta.env")),
                    null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(),
                              ["http://localhost:8080/callback", "http://localhost:8888/callback"],
                              ["http://localhost:8080/", "http://localhost:8888/"])
        }
    }

    @Test
    void createServiceApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                                        // GET /api/v1/authorizationServers
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
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
                    "",  // default of "test-project"
                    "4", // "native" type of app choice
                    "",  // default callback "localhost:/callback"
                    "",  // default post logout redirect
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
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
                                        jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
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
                    "", // generic OIDC app
                    "", // default "web" type of app choice
                    "", // default of "test-project"
                    "", // default callback "http://localhost:8080/login/oauth2/code/okta"
                    "", // default post logout redirect
            ]

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommandWithInput(input, "--color=never", "apps", "create", "spring-boot")

            assertThat result, resultMatches(0, allOf(
                                                            containsString("Created OIDC application, client-id: test-id"),
                                                            containsString("Okta application configuration has been written to"),
                                                            containsString("Enter your Redirect URI(s) [http://localhost:8080/login/oauth2/code/okta]:"),
                                                            containsString("Enter your Post Logout Redirect URI(s) [http://localhost:8080/]:"),
                                                            containsString("application.properties")),
                                                        null)

            mockWebServer.takeRequest() // auth list request
            mockWebServer.takeRequest() // check if app exists
            verifyRedirectUri(mockWebServer.takeRequest(), ["http://localhost:8080/login/oauth2/code/okta"])
        }
    }

    private void verifyRedirectUri(RecordedRequest request, List<String> expectedRedirectUris, List<String> expectedPostLogoutRedirectUris = null) {
        verify(request, "POST", "/api/v1/apps")
        def body = new JsonSlurper().parseText(request.getBody().readUtf8())
        List<String> redirectUris = body.settings.oauthClient.redirect_uris
        assertThat redirectUris, equalTo(expectedRedirectUris)

        if (expectedPostLogoutRedirectUris != null) {
            List<String> postLogoutUris = body.settings.oauthClient.post_logout_redirect_uris
            assertThat postLogoutUris, equalTo(expectedPostLogoutRedirectUris)
        }
    }

    private void verifyTrustedOrigins(RecordedRequest request, String expectedUri) {
        verify(request, "POST", "/api/v1/trustedOrigins")
        def body = new JsonSlurper().parse(request.getBody().inputStream())
        assertThat body.origin, equalTo(expectedUri)
    }
}
