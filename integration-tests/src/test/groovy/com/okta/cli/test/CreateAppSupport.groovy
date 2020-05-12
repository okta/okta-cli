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

import com.okta.sdk.authc.credentials.ClientCredentials
import com.okta.sdk.client.Client
import com.okta.sdk.impl.client.DefaultClientBuilder
import com.okta.sdk.impl.ds.JacksonMapMarshaller
import com.okta.sdk.impl.util.BaseUrlResolver
import com.okta.sdk.resource.application.*
import com.okta.sdk.resource.group.Group
import com.okta.sdk.resource.group.GroupProfile
import groovy.json.JsonSlurper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matcher
import org.yaml.snakeyaml.Yaml

import java.nio.charset.StandardCharsets

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.mock

trait CreateAppSupport {

    List<String> getRegistrationInputs() {
        return[
                "test-first",
                "test-last",
                "test-email@example.com",
                "test co"
        ]
    }

    MockResponse jsonRequest(String json) {
        return new MockResponse()
                .setBody(json)
                .setHeader("Content-Type", "application/json")
    }

    void verifyOrgCreateRequest(RecordedRequest request, String firstName = "test-first", String lastName = "test-last", String email = "test-email@example.com", String company = "test co") {
        assertThat request.getRequestLine(), equalTo("POST /create HTTP/1.1")
        assertThat request.getHeader("Content-Type"), equalTo("application/json")
        Map body = new JsonSlurper().parse(request.getBody().readByteArray(), StandardCharsets.UTF_8.toString())
        assertThat body, equalTo([
                firstName: firstName,
                lastName: lastName,
                email: email,
                organization: company
        ])
    }

    void verifyUserAgent(RecordedRequest request) {
        assertThat request.getHeader("User-Agent"), containsString("okta-cli")
    }

    void verifyJsonRequestBody(RecordedRequest request, Matcher<String>... matchers) {
        assertThat request.getHeader("Content-Type"), equalTo("application/json")
        assertThat(request.getBody().readUtf8(), allOf(matchers))
    }

    Map parseYaml(File oktaConfigFile) {
        Yaml yaml = new Yaml()
        return yaml.load(oktaConfigFile.text)
    }

    String everyoneGroup(String baseUrl) {

        Client client = client(baseUrl)
        Group group = client.instantiate(Group)
        group.setProfile(client.instantiate(GroupProfile))
        group.getProfile().setName("Everyone")
        group.getProfile().setDescription("Everyone Test Group")
        group.put("id", "every1-id")

        return toString(group)
    }

    String application(String label = "test-project", List<String> redirectUris = ["http://localhost:8080/callback","http://localhost:8080/login/oauth2/code/okta"]) {

        Client client = client()
        OpenIdConnectApplication app = client.instantiate(OpenIdConnectApplication)
        app.put("id", "test-app-id")
        app.setSettings(client.instantiate(OpenIdConnectApplicationSettings)
                .setOAuthClient(client.instantiate(OpenIdConnectApplicationSettingsClient)
                        .setGrantTypes([OAuthGrantType.AUTHORIZATION_CODE])
                        .setApplicationType(OpenIdConnectApplicationType.WEB)
                        .setRedirectUris(redirectUris)
                        .setResponseTypes([OAuthResponseType.CODE])))

        app.setLabel(label)
        app.setSignOnMode(ApplicationSignOnMode.BROWSER_PLUGIN)
        app.put("name", "oidc_client")

        return toString(app)
    }

    String toString(Map resource) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        new JacksonMapMarshaller().marshal(baos, resource)
        return baos.toString(StandardCharsets.UTF_8)
    }

    Client client(String baseUrl = "https://not-used.example.com") {
        def clientCredentials = mock(ClientCredentials)

        Client client = new DefaultClientBuilder()
                .setBaseUrlResolver(new StubBaseUrlResolver(baseUrl))
                .setClientCredentials(clientCredentials)
                .build()

        return client
    }

    void verifyOktaConfig(CommandRunner.Result result, String orgBaseUrl, String token = "fake-test-token") {
        File oktaConfigFile = new File(result.homeDir, ".okta/okta.yaml")
        assertThat oktaConfigFile, new OktaConfigMatcher(orgBaseUrl, token)
    }

    void verifyApplicationYml(CommandRunner.Result result, String issuer, String clientId, String clientSecret) {
        File appProps = new File(result.workDir, "src/main/resources/application.yml")
        assertThat "Expected ${appProps} to exist", appProps.exists()

        assertThat parseYaml(appProps), equalTo([
                okta: [
                        oauth2: [
                                "client-secret": clientSecret,
                                "client-id": clientId,
                                issuer: issuer
                        ]
                ]
        ])
    }

    String oidcAppCredentials() {
        return """{  "client_id": "test-client-id", "client_secret": "test-client-secret" }"""
    }

    static class StubBaseUrlResolver implements BaseUrlResolver {

        private final baseUrl

        StubBaseUrlResolver(baseUrl) {
            this.baseUrl = baseUrl
        }

        @Override
        String getBaseUrl() {
            return baseUrl
        }
    }
}
