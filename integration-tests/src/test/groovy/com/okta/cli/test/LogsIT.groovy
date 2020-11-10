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

import groovy.json.JsonOutput
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.testng.annotations.Test

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.greaterThan

class LogsIT implements MockWebSupport {

    @Test
    void listLogs() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
            jsonRequest( [
                [ published: "2020-01-12T12:30:15.100Z", uuid: "event-id-1", severity: "INFO", displayMessage: "entry - 1", outcome: [result: "SUCCESS"] ],
                [ published: "2020-01-12T12:30:15.110Z", uuid: "event-id-2", severity: "WARN", displayMessage: "entry - 2", outcome: [result: "DENY"] ]
            ]).setHeader("link", "<${url(mockWebServer, '/api/v1/logs?after=4562789006123_1')}>; rel=\"next\""),
            jsonRequest( [] ).setHeader("link", "<${url(mockWebServer, '/api/v1/logs?after=4562789006123_1')}>; rel=\"next\"") ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommand("-Dokta.testing.disableHttpsCheck=true", "logs", "--color=never")

            assertThat result, resultMatches(0, allOf(
                        containsString("Time                      Severity  Status     Message"),
                        containsString("2020-01-12T12:30:15.100Z  INFO      SUCCESS    entry - 1  ${url(mockWebServer,  "/api/v1/logs?filter=uuid+eq+%22event-id-1%22")}"),
                        containsString("2020-01-12T12:30:15.110Z  WARN      DENY       entry - 2  ${url(mockWebServer, "/api/v1/logs?filter=uuid+eq+%22event-id-2%22")}")
                    ),
                    null)
        }
    }

    @Test
    void followLogs() {
        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest( [
                        [ published: "2020-01-12T12:30:15.100Z", uuid: "event-id-1", severity: "INFO", displayMessage: "entry - 1", outcome: [result: "SUCCESS"] ],
                        [ published: "2020-01-12T12:30:15.110Z", uuid: "event-id-2", severity: "WARN", displayMessage: "entry - 2", outcome: [result: "DENY"] ]
                ]).setHeader("link", "<${url(mockWebServer, '/api/v1/logs?after=4562789006123_1')}>; rel=\"next\""),
                jsonRequest( [] ).setHeader("link", "<${url(mockWebServer, '/api/v1/logs?after=4562789006123_1')}>; rel=\"next\""),
                jsonRequest( [
                        [ published: "2020-01-12T12:30:15.260Z", uuid: "event-id-3", severity: "INFO", displayMessage: "entry - 3", outcome: [result: "SUCCESS"] ]
        ]).setHeader("link", "<${url(mockWebServer, '/api/v1/logs?after=4562789006123_1')}>; rel=\"next\"")]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            def result = new CommandRunner()
                    .withSdkConfig(url(mockWebServer,"/"))
                    .runCommand("-Dokta.testing.disableHttpsCheck=true", "logs", "--color=never", "-f")

            assertThat result, resultMatches(greaterThan(0), allOf(
                    containsString("Time                      Severity  Status     Message"),
                    containsString("2020-01-12T12:30:15.100Z  INFO      SUCCESS    entry - 1  ${url(mockWebServer, "/api/v1/logs?filter=uuid+eq+%22event-id-1%22")}"),
                    containsString("2020-01-12T12:30:15.110Z  WARN      DENY       entry - 2  ${url(mockWebServer, "/api/v1/logs?filter=uuid+eq+%22event-id-2%22")}"),
                    containsString("2020-01-12T12:30:15.260Z  INFO      SUCCESS    entry - 3  ${url(mockWebServer, "/api/v1/logs?filter=uuid+eq+%22event-id-3%22")}")
            ),
                    null)
        }
    }

    MockResponse jsonRequest(Object obj) {
        return jsonRequest(JsonOutput.toJson(obj))
    }

    MockResponse jsonRequest(String json) {
        return new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
    }
}
