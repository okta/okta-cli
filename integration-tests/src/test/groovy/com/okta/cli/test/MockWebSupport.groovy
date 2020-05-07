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

}