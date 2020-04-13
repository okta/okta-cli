package com.okta.cli.test

import groovy.json.JsonOutput
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.testng.annotations.Test
import org.yaml.snakeyaml.Yaml

import static org.hamcrest.Matchers.*
import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat

class LoginIT implements MockWebSupport{

    @Test
    void basicLoginTest() {

        List<String> input = [
                "https://okta.example.com",
                "test-token"
        ]

        def result = new CommandRunner().runCommandWithInput(input, "login")
        assertThat result, resultMatches(0, allOf(containsString("Okta Org URL:"), containsString("Enter your Okta API token")), emptyString())

        File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
        assertThat oktaConfigFile, new OktaConfigMatcher("https://okta.example.com", "test-token")
    }

    @Test
    void invalidOrgUrl() {
        List<String> input = [ "http://require-tls.example.com" ]
        def result = new CommandRunner().runCommandWithInput(input, "login")
        assertThat result, resultMatches(1, null, containsString("Your Okta URL must start with https"))
    }

    @Test
    void invalidTokenUrl() {
        List<String> input = [
                "https://require-tls.example.com",
                "{apiToken}" // a common typo
        ]
        def result = new CommandRunner().runCommandWithInput(input, "login")
        assertThat result, resultMatches(1, null, containsString("Replace {apiToken} with your Okta API token"))
    }
}
