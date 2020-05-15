/*
 * Copyright 2018-Present Okta, Inc, Inc.
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

class AppsIT implements MockWebSupport {

    @Test
    void listApps() {
        List<MockResponse> responses = [new MockResponse()
                                            .setBody('[{ "id": "app-id-1", "label": "App 1" }, { "id": "app-id-2", "label": "App 2" }]')
                                            .setHeader("Content-Type", "application/json")]

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            CommandRunner commandRunner = new CommandRunner() {
                @Override
                protected void setupHomeDir(File homeDir) {

                    File file = new File(homeDir,".okta/okta.yaml")
                    file.getParentFile().mkdir()
                    file.write "okta:\n"
                    file << "  client:\n"
                    file << "    orgUrl: ${mockWebServer.url("/")}\n"
                    file << "    token: some-test-token\n"
                }
            }

            def result = commandRunner.runCommand("-Dokta.testing.disableHttpsCheck=true", "apps")
            assertThat result, resultMatches(0, allOf(containsString("app-id-1\tApp 1"),
                                                               containsString("app-id-2\tApp 2")),
                                                          null)
        }
    }
}