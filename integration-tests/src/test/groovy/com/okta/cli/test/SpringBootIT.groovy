package com.okta.cli.test


import okhttp3.mockwebserver.MockWebServer
import org.testng.annotations.Test

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString

class SpringBootIT implements MockWebSupport, CreateAppSupport {

    @Test
    void happyPath() {

        MockWebServer mockWebServer = createMockServer()
        mockWebServer.with {

            String orgBaseUrl = mockWebServer.url("")

            mockWebServer.enqueue(jsonRequest("""{ "orgUrl": "${orgBaseUrl}", "email": "test-email@example.com", "apiToken": "fake-test-token" }"""))

            // create new app
            mockWebServer.enqueue(jsonRequest("[]")) // GET /api/v1/apps?q=<my-app-name>
            mockWebServer.enqueue(jsonRequest(application())) // POST /api/v1/apps

            // get Everyone group id
            mockWebServer.enqueue(jsonRequest("[${everyoneGroup()}]")) // GET /api/v1/groups?q=everyone
            mockWebServer.enqueue(jsonRequest("{}")) // PUT /api/v1/apps/test-app-id/groups/every1-id

            // assign app to group
            mockWebServer.enqueue(jsonRequest(oidcAppCredentials())) // GET /api/v1/internal/apps/test-app-id/settings/clientcreds

            // TODO need to figure out how to setup the trust store in the native binary for testing?
            def result = new CommandRunner(mockWebServer.url("/").toString()).runCommandWithInput(registrationInputs, "-Dokta.testing.disableHttpsCheck=true", "spring-boot")
            assertThat result, resultMatches(0, allOf(containsString("Check your email address to verify your account"),
                                                               containsString("OrgUrl: ${orgBaseUrl}")),
                                                         null) // TODO warnings are printed because this test includes disableHttpsCheck

            // verify collected info was sent
            verifyOrgCreateRequest(mockWebServer.takeRequest())

            // validate the user-agent is correct
            verifyUserAgent(mockWebServer.takeRequest())

            verifyJsonRequestBody(mockWebServer.takeRequest(), containsString("http://localhost:8080/authorization-code/callback"),
                                                               containsString("http://localhost:8080/login/oauth2/code/okta"))

            // verify okta.yaml was configured with token
            verifyOktaConfig(result, orgBaseUrl)

            // verify application.yml
            verifyApplicationYml(result, "${orgBaseUrl}/oauth2/default".toString(), "test-client-id", "test-client-secret")
        }
    }
}