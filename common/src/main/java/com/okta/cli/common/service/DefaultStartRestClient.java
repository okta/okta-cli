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
import com.okta.cli.common.model.SamplesListings;
import com.okta.cli.common.model.VersionInfo;
import com.okta.commons.lang.ApplicationInfo;
import com.okta.sdk.error.Error;
import com.okta.sdk.impl.ds.JacksonMapMarshaller;
import com.okta.sdk.impl.ds.MapMarshaller;
import com.okta.sdk.impl.error.DefaultError;
import com.okta.sdk.resource.ResourceException;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DefaultStartRestClient implements RestClient, StartRestClient {

    private static final String APPLICATION_JSON = "application/json";

    private static final String USER_AGENT_STRING = ApplicationInfo.get().entrySet().stream()
            .map(e -> e.getKey() + "/" + e.getValue())
            .collect(Collectors.joining(" "));

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final MapMarshaller mapMarshaller = new JacksonMapMarshaller();

    private final String baseUrl;

    public DefaultStartRestClient() {
        this(Settings.getCliApiUrl());
    }

    public DefaultStartRestClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

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
                throw new ResourceException(error(content, response.getStatusLine().getStatusCode()));
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
                throw new ResourceException(error(content, response.getStatusLine().getStatusCode()));
            }
        }
    }

    private String fullUrl(String relative) {
        return baseUrl + relative;
    }

    private Error error(InputStream content, int statusCode) {
        Map<String, Object> data = mapMarshaller.unmarshal(content, Collections.emptyMap());
        DefaultError error = new DefaultError(data);
        if (error.getStatus() < 0) {
            error.setStatus(statusCode);
        }
        return error;
    }
}
