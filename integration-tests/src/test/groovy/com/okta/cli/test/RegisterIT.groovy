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
import java.time.Instant

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class RegisterIT implements MockWebSupport {

    @Test
    void happyPath() {

        List<MockResponse> responses = [
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "identifier": "test-id" }'),
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "apiToken": "fake-test-token" }')
        ]

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "test-first",
                    "test-last",
                    "test-email@example.com",
                    "test co",
                    "123456"
            ]

            def result = new CommandRunner(mockWebServer.url("/").toString()).runCommandWithInput(input, "register")
            assertThat result, resultMatches(0, allOf(containsString("An email has been sent to you with a verification code."), containsString("Verification code")), emptyString())


            RecordedRequest request = mockWebServer.takeRequest()
            assertThat request.getRequestLine(), equalTo("POST /create HTTP/1.1")
            assertThat request.getHeader("Content-Type"), is("application/json")
            Map body = new JsonSlurper().parse(request.getBody().readByteArray(), StandardCharsets.UTF_8.toString())
            assertThat body, equalTo([
                    firstName: "test-first",
                    lastName: "test-last",
                    email: "test-email@example.com",
                    organization: "test co"
            ])

            File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
            assertThat oktaConfigFile, new OktaConfigMatcher("https://result.example.com", "fake-test-token")
        }
    }

    @Test
    void invalidCodeTest() {
        List<MockResponse> responses = [
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "identifier": "test-id" }'),
                jsonRequest(new ErrorResponse()
                        .setError("Invalid passcode")
                        .setMessage("Test message")
                        .setCauses(["error 1", "error 2"])
                        .setStatus(401),
                        401
                ),
                jsonRequest('{ "orgUrl": "https://result.example.com", "email": "test-email@example.com", "apiToken": "fake-test-token" }')
        ]

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "test-first",
                    "test-last",
                    "test-email@example.com",
                    "test co",
                    "123456",
                    "654321"
            ]

            def result = new CommandRunner(mockWebServer.url("/").toString()).runCommandWithInput(input, "register")
            assertThat result, resultMatches(0, allOf(containsString("An email has been sent to you with a verification code."), containsString("Verification code")), emptyString())

            RecordedRequest request = mockWebServer.takeRequest()
            assertThat request.getRequestLine(), equalTo("POST /create HTTP/1.1")
            assertThat request.getHeader("Content-Type"), is("application/json")
            Map body = new JsonSlurper().parse(request.getBody().readByteArray(), StandardCharsets.UTF_8.toString())
            assertThat body, equalTo([
                    firstName: "test-first",
                    lastName: "test-last",
                    email: "test-email@example.com",
                    organization: "test co"
            ])

            File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
            assertThat oktaConfigFile, new OktaConfigMatcher("https://result.example.com", "fake-test-token")
        }
    }
}
