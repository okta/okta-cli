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
package com.okta.cli.common.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.cli.common.RestException;
import com.okta.cli.common.model.ErrorResponse;
import com.okta.cli.common.model.SamplesListings;
import com.okta.cli.common.model.VersionInfo;
import com.okta.commons.lang.ApplicationInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DefaultStartRestClient implements RestClient, StartRestClient {

    /**
     * The base URL of the service used to create a new Okta account.
     * This value is NOT exposed as a plugin parameter, but CAN be set using the env var {@code OKTA_CLI_BASE_URL}.
     */
    private static final String DEFAULT_API_BASE_URL = "https://start.okta.dev/";

    private static final String APPLICATION_JSON = "application/json";

    private static final String USER_AGENT_STRING = ApplicationInfo.get().entrySet().stream()
            .map(e -> e.getKey() + "/" + e.getValue())
            .collect(Collectors.joining(" "));

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public VersionInfo getVersionInfo() throws IOException, RestException {
        return get("/versions/cli", VersionInfo.class);
    }

    @Override
    public List<SamplesListings.OktaSample> listSamples() throws IOException, RestException {
        return get("/samples", SamplesListings.class).getItems();
    }

    @Override
    public <T> T get(String url, Class<T> responseType) throws RestException, IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet getMethod = new HttpGet(fullUrl(url));

            getMethod.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
            getMethod.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            getMethod.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_STRING);

            HttpResponse response = httpClient.execute(getMethod);

            Header contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
            if (contentTypeHeader == null || !contentTypeHeader.getValue().contains(APPLICATION_JSON)) {
                log.warn("Content-Type header was NOT set to {}, parsing the response may fail", APPLICATION_JSON);
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

    public <T> T post(String url, Object body, Class<T> responseType) throws RestException, IOException {
        String postBody = objectMapper.writeValueAsString(body);
        return post(url, postBody, responseType);
    }

    @Override
    public <T> T post(String url, String body, Class<T> responseType) throws RestException, IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(fullUrl(url));

            post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            post.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
            post.setHeader(HttpHeaders.ACCEPT, APPLICATION_JSON);
            post.setHeader(HttpHeaders.USER_AGENT, USER_AGENT_STRING);

            HttpResponse response = httpClient.execute(post);

            Header contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
            if (contentTypeHeader == null || !contentTypeHeader.getValue().contains(APPLICATION_JSON)) {
                log.warn("Content-Type header was NOT set to {}, parsing the response may fail", APPLICATION_JSON);
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

    private String fullUrl(String relative) {
        return getApiBaseUrl() + relative;
    }

    private String getApiBaseUrl() {
        return System.getenv().getOrDefault("OKTA_CLI_BASE_URL", DEFAULT_API_BASE_URL);
    }
}
