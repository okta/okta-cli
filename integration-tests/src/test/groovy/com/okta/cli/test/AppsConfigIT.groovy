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

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.testng.annotations.Test

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString

class AppsConfigIT implements MockWebSupport {

    @Test
    void happyPath() {
        List<MockResponse> responses = [new MockResponse()
                                            .setBody('{ "id": "test-app-id", "label": "test-app-name", "signOnMode": "OPENID_CONNECT" }')
                                            .setHeader("Content-Type", "application/json"),
                                        new MockResponse()
                                            .setBody('{ "client_id": "test-id", "client_secret": "test-secret" }')
                                            .setHeader("Content-Type", "application/json"),
                                        new MockResponse()
                                            .setBody('[{ "id": "test-as", "name": "test-as-name", "issuer": "https://issuer.example.com" }]')
                                            .setHeader("Content-Type", "application/json")]

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommand( "apps", "config", "--app", "test-app")

            assertThat result, resultMatches(0, allOf(containsString("Name:          test-app-name"),
                                                               containsString("Client Id:     test-id"),
                                                               containsString("Client Secret: test-secret"),
                                                               containsString("Issuer:        https://issuer.example.com")),
                                                          null)
        }
    }
}
