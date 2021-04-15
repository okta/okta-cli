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

import com.okta.cli.common.model.ErrorResponse
import groovy.json.JsonSlurper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.testng.annotations.Test

import java.nio.charset.StandardCharsets

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class RegisterIT implements MockWebSupport {

    @Test
    void happyPath() {

        List<MockResponse> responses = [
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "developerOrgCliToken": "test-id" }'),
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "apiToken": "fake-test-token", "status": "ACTIVE" }')
        ]

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "test-first",
                    "test-last",
                    "test-email@example.com",
                    "Petoria",
            ]

            def result = new CommandRunner(url(mockWebServer, "/")).runCommandWithInput(input, "register", "--verbose")
            assertThat result, resultMatches(0, allOf(containsString("An account activation email has been sent to you.")), emptyString())


            RecordedRequest request = mockWebServer.takeRequest()
            assertThat request.getRequestLine(), equalTo("POST /api/v1/registration/reg405abrRAkn0TRf5d6/register HTTP/1.1")
            assertThat request.getHeader("Content-Type"), is("application/json")
            Map body = new JsonSlurper().parse(request.getBody().readByteArray(), StandardCharsets.UTF_8.toString())
            assertThat body, equalTo([
                userProfile: [
                    firstName: "test-first",
                    lastName: "test-last",
                    email: "test-email@example.com",
                    country: "Petoria"
                ]
            ])

            File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
            assertThat oktaConfigFile, new OktaConfigMatcher("https://result.example.com", "fake-test-token")
        }
    }

    @Test
    void existingConfigFile_overwrite() {

        List<MockResponse> responses = [
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "developerOrgCliToken": "test-id" }'),
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "apiToken": "fake-test-token", "status": "ACTIVE" }\') }')
        ]

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "y", // overwrite config
                    "test-first",
                    "test-last",
                    "test-email@example.com",
                    "Petoria"
            ]

            CommandRunner runner = new CommandRunner(url(mockWebServer, "/"))
                    .withHomeDirectory {
                        File oktaYaml = new File(it, ".okta/okta.yaml")
                        oktaYaml.getParentFile().mkdirs()
                        oktaYaml.write("""
okta:
  client:
    orgUrl: https://test.example.com
    token: test-token
""")
                    }

            def result = runner.runCommandWithInput(input, "register")
            assertThat result, resultMatches(0, containsString("An account activation email has been sent to you."), emptyString())

            RecordedRequest request = mockWebServer.takeRequest()
            assertThat request.getRequestLine(), equalTo("POST /api/v1/registration/reg405abrRAkn0TRf5d6/register HTTP/1.1")
            assertThat request.getHeader("Content-Type"), is("application/json")
            Map body = new JsonSlurper().parse(request.getBody().readByteArray(), StandardCharsets.UTF_8.toString())
            assertThat body, equalTo([
                userProfile: [
                    firstName: "test-first",
                    lastName: "test-last",
                    email: "test-email@example.com",
                    country: "Petoria"
                ]
            ])

            File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
            assertThat oktaConfigFile, new OktaConfigMatcher("https://result.example.com", "fake-test-token")
        }
    }

    @Test
    void existingConfigFile_noOverwrite() {

            List<String> input = [
                    "no" // no overwrite
            ]

            CommandRunner runner = new CommandRunner()
                    .withHomeDirectory {
                        File oktaYaml = new File(it, ".okta/okta.yaml")
                        oktaYaml.getParentFile().mkdirs()
                        oktaYaml.write("""
okta:
  client:
    orgUrl: https://test.example.com
    token: test-token
""")
                    }

        def result = runner.runCommandWithInput(input, "register")
        assertThat result, resultMatches(1, containsString("Overwrite configuration file?"), containsString("User canceled"))

        File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
        assertThat oktaConfigFile, new OktaConfigMatcher("https://test.example.com", "test-token")

    }

    @Test
    void pollingTest() {
        List<MockResponse> responses = [
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "developerOrgCliToken": "test-id" }'),
                jsonRequest('{ "status": "PENDING" }'),
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "apiToken": "fake-test-token", "status": "ACTIVE" }')
        ]

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "test-first",
                    "test-last",
                    "test-email@example.com",
                    "Petoria"
            ]

            def result = new CommandRunner(url(mockWebServer, "/")).runCommandWithInput(input, "register")
            assertThat result, resultMatches(0, containsString("An account activation email has been sent to you."), emptyString())

            RecordedRequest request = mockWebServer.takeRequest()
            assertThat request.getRequestLine(), equalTo("POST /api/v1/registration/reg405abrRAkn0TRf5d6/register HTTP/1.1")
            assertThat request.getHeader("Content-Type"), is("application/json")
            Map body = new JsonSlurper().parse(request.getBody().readByteArray(), StandardCharsets.UTF_8.toString())
            assertThat body, equalTo([
                userProfile: [
                    firstName: "test-first",
                    lastName: "test-last",
                    email: "test-email@example.com",
                    country: "Petoria"
                ]
            ])

            File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
            assertThat oktaConfigFile, new OktaConfigMatcher("https://result.example.com", "fake-test-token")
        }
    }
}
