package com.okta.cli.test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.testng.annotations.Test

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat

class AppsDeleteIT implements MockWebSupport, CreateAppSupport {

    @Test
    void deleteMissingApp() {

        MockWebServer mockWebServer = createMockServer()
        List<MockResponse> responses = [
                jsonRequest('{}')
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
//                    "",  // default of "test-project"
//                    "2", // type of app choice "spa"
//                    "",  // default callback "http://localhost:8080/callback"
//                    "",  // default post logout redirect
            ]

            def result = new CommandRunner()
                    .withSdkConfig(mockWebServer.url("/").toString())
                    .runCommandWithInput(input, "--color=never", "apps", "delete", "app-id1")

            assertThat result, resultMatches(0, null,
                    null)

            verify(mockWebServer.takeRequest(), "GET", "/api/v1/apps/app-id1")

        }
    }
}
