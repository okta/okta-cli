/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.cli.common.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.commons.lang.ApplicationInfo;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DefaultOktaOrganizationCreator implements OktaOrganizationCreator {

    private static final String APPLICATION_JSON = "application/json";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultOktaOrganizationCreator.class);

    private static final String USER_AGENT_STRING = ApplicationInfo.get().entrySet().stream()
            .map(e -> e.getKey() + "/" + e.getValue())
            .collect(Collectors.joining(" "));

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OrganizationResponse createNewOrg(String apiBaseUrl, OrganizationRequest orgRequest) throws IOException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(apiBaseUrl + "/create");

            String postBody = objectMapper.writeValueAsString(orgRequest);

            post.setEntity(new StringEntity(postBody, StandardCharsets.UTF_8));
            post.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
            post.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            post.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_STRING);

            HttpResponse response = httpClient.execute(post);

            Header contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
            if (contentTypeHeader == null || !contentTypeHeader.getValue().contains(APPLICATION_JSON)) {
                LOG.warn("Content-Type header was NOT set to {}, parsing the response may fail", APPLICATION_JSON);
            }

            InputStream content = response.getEntity().getContent();
            String body = CharStreams.toString(new InputStreamReader(content, StandardCharsets.UTF_8)); // use input stream directly
            return objectMapper.reader().readValue(new JsonFactory().createParser(body), OrganizationResponse.class);
        }
    }

    @Override
    public OrganizationResponse verifyNewOrg(String apiBaseUrl, String identifier, String code) throws IOException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(apiBaseUrl + "/verify/" + identifier);

            String postBody = "{\"code\":\"" + code + "\"}";

            post.setEntity(new StringEntity(postBody, StandardCharsets.UTF_8));
            post.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
            post.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            post.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_STRING);

            HttpResponse response = httpClient.execute(post);

            Header contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
            if (contentTypeHeader == null || !contentTypeHeader.getValue().contains(APPLICATION_JSON)) {
                LOG.warn("Content-Type header was NOT set to {}, parsing the response may fail", APPLICATION_JSON);
            }

            InputStream content = response.getEntity().getContent();
            String body = CharStreams.toString(new InputStreamReader(content, StandardCharsets.UTF_8)); // use input stream directly
            return objectMapper.reader().readValue(new JsonFactory().createParser(body), OrganizationResponse.class);

            // TODO handle errors and throw typed exceptions to the user can retry
        }
    }
}