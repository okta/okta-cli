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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.cli.common.FactorVerificationException;
import com.okta.cli.common.RestException;
import com.okta.cli.common.model.ErrorResponse;
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
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DefaultOktaOrganizationCreator implements OktaOrganizationCreator {

    private static final String APPLICATION_JSON = "application/json";
    private static final Logger LOG = LoggerFactory.getLogger(DefaultOktaOrganizationCreator.class);

    private static final String USER_AGENT_STRING = ApplicationInfo.get().entrySet().stream()
            .map(e -> e.getKey() + "/" + e.getValue())
            .collect(Collectors.joining(" "));

    ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @Override
    public OrganizationResponse createNewOrg(String apiBaseUrl, OrganizationRequest orgRequest) throws RestException, IOException {

        String url = apiBaseUrl + "/create";
        String postBody = objectMapper.writeValueAsString(orgRequest);
        return post(url, postBody, OrganizationResponse.class);
    }

    @Override
    public OrganizationResponse verifyNewOrg(String apiBaseUrl, String identifier, String code) throws FactorVerificationException, IOException {

        String url = apiBaseUrl + "/verify/" + identifier;
        String postBody = "{\"code\":\"" + code + "\"}";

        try {
            return post(url, postBody, OrganizationResponse.class);
        } catch (RestException e) {
            throw new FactorVerificationException(e.getErrorResponse(), e);
        }
    }

    private <T> T post(String url, String body, Class<T> responseType) throws RestException, IOException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);

            post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            post.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
            post.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            post.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_STRING);

            HttpResponse response = httpClient.execute(post);

            Header contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
            if (contentTypeHeader == null || !contentTypeHeader.getValue().contains(APPLICATION_JSON)) {
                LOG.warn("Content-Type header was NOT set to {}, parsing the response may fail", APPLICATION_JSON);
            }

            InputStream content = response.getEntity().getContent();

            // check for error
            if (response.getStatusLine().getStatusCode() == 200) {
                return objectMapper.reader().readValue(content, responseType);
            } else {
                // assume error
                ErrorResponse error = objectMapper.reader().readValue(content, ErrorResponse.class);
                throw new RestException(error);
            }
        }
    }
}