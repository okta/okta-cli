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

trait MockWebSupport {

    MockWebServer createMockServer() {

        def mockServer = new MockWebServer()
        return mockServer
    }

    def withMock(List<MockResponse> responses, Closure closure) {
        MockWebServer mockWebServer = createMockServer()
        try {
            responses.forEach { mockWebServer.enqueue(it) }
            return closure.call()
        } finally {
            mockWebServer.close()
        }
    }

    MockResponse jsonRequest(String json) {
        return new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
    }

}