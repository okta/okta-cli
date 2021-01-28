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
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.not

class AppsDeleteIT implements MockWebSupport, CreateAppSupport {

    @Test
    void deleteActiveApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{"id": "app-id1", "status": "ACTIVE"}'),
                new MockResponse().setResponseCode(204),
                new MockResponse().setResponseCode(204)
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "y"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "--verbose", "app-id1")

            assertThat result, resultMatches(0, allOf(
                    containsString("Deactivate and delete application 'app-id1'? [y/N]"),
                    containsString("Application 'app-id1' has been deleted")),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")
            verify(mockWebServer.takeRequest(), "POST", "/api/v1/apps/app-id1/lifecycle/deactivate")
            verify(mockWebServer.takeRequest(), "DELETE", "/api/v1/apps/app-id1")
            assertThat mockWebServer.requestCount, equalTo(3)
        }
    }
    @Test
    void forceDeleteActiveApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{"id": "app-id1", "status": "ACTIVE"}'),
                new MockResponse().setResponseCode(204),
                new MockResponse().setResponseCode(204)
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = []

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "--force", "app-id1")

            assertThat result, resultMatches(0, allOf(
                    not(containsString("Deactivate and delete application 'app-id1'? [y/N]")),
                    containsString("Application 'app-id1' has been deleted")),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")
            verify(mockWebServer.takeRequest(), "POST", "/api/v1/apps/app-id1/lifecycle/deactivate")
            verify(mockWebServer.takeRequest(), "DELETE", "/api/v1/apps/app-id1")
            assertThat mockWebServer.requestCount, equalTo(3)
        }
    }


    @Test
    void deleteInactiveApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{"id": "app-id1", "status": "INACTIVE"}'),
                new MockResponse().setResponseCode(204)
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "y"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "--verbose", "app-id1")

            assertThat result, resultMatches(0, allOf(
                        containsString("Deactivate and delete application 'app-id1'? [y/N]"),
                        containsString("Application 'app-id1' has been deleted")),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")
            verify(mockWebServer.takeRequest(), "DELETE", "/api/v1/apps/app-id1")
            assertThat mockWebServer.requestCount, equalTo(2)
        }
    }

    @Test
    void deleteAlreadyDeletedApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{"id": "app-id1", "status": "DELETED"}')
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = []

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "--verbose", "app-id1")

            assertThat result, resultMatches(1, containsString("Application 'app-id1' has already been marked for deletion"),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")
            assertThat mockWebServer.requestCount, equalTo(1)
        }
    }

    @Test
    void cancelDeleteApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{"id": "app-id1", "status": "ACTIVE"}'),
                new MockResponse().setResponseCode(204)
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "n"
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "--verbose", "app-id1")

            assertThat result, resultMatches(1, allOf(
                    containsString("Deactivate and delete application 'app-id1'? [y/N]")),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")
            assertThat mockWebServer.requestCount, equalTo(1)
        }
    }

    @Test
    void nonInteractiveDeleteApp() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{"id": "app-id1", "status": "ACTIVE"}')
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = []

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "--batch", "app-id1")

            assertThat result, resultMatches(1, allOf(
                    containsString("Application 'app-id1' has not been deactivated, use '--force' to delete it"),
                    not(containsString("Deactivate and delete application 'app-id1'? [y/N]"))),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")
            assertThat mockWebServer.requestCount, equalTo(1)
        }
    }

    @Test
    void deleteMultipleFirstFails() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{"id": "app-id1", "status": "ACTIVE"}'),
                new MockResponse().setResponseCode(400),
                jsonRequest('{"id": "app-id2", "status": "ACTIVE"}'),
                new MockResponse().setResponseCode(204),
                new MockResponse().setResponseCode(204)
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    "y",
                    "y",
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "--verbose", "app-id1", "app-id2")

            assertThat result, resultMatches(1, allOf(
                    containsString("Deactivate and delete application 'app-id1'? [y/N]"),
                    containsString("Failed to delete application: 'app-id1'"),
                    containsString("Deactivate and delete application 'app-id2'? [y/N]"),
                    containsString("Application 'app-id2' has been deleted")),
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")
            verify(mockWebServer.takeRequest(), "POST", "/api/v1/apps/app-id1/lifecycle/deactivate")
            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id2")
            verify(mockWebServer.takeRequest(), "POST", "/api/v1/apps/app-id2/lifecycle/deactivate")
            verify(mockWebServer.takeRequest(), "DELETE", "/api/v1/apps/app-id2")
            assertThat mockWebServer.requestCount, equalTo(5)
        }
    }
}
