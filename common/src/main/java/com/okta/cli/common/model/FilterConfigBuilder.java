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
package com.okta.cli.common.model;

import com.okta.cli.common.config.MutablePropertySource;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FilterConfigBuilder {

    private static final String CLI_OKTA_ORG_URL = "CLI_OKTA_ORG_URL";
    private static final String CLI_OKTA_ISSUER = "CLI_OKTA_ISSUER";
    private static final String CLI_OKTA_ISSUER_ID = "CLI_OKTA_ISSUER_ID";
    private static final String CLI_OKTA_CLIENT_ID = "CLI_OKTA_CLIENT_ID";
    private static final String CLI_OKTA_CLIENT_SECRET = "CLI_OKTA_CLIENT_SECRET";
    private static final String CLI_OKTA_REVERSE_DOMAIN = "CLI_OKTA_REVERSE_DOMAIN";

    private final Map<String, String> filterValues = new HashMap<>();

    public FilterConfigBuilder setClientId(String clientId) {
        filterValues.put(CLI_OKTA_CLIENT_ID, clientId);
        return this;
    }

    public FilterConfigBuilder setClientSecret(String clientSecret) {
        filterValues.put(CLI_OKTA_CLIENT_SECRET, clientSecret);
        return this;
    }
    public FilterConfigBuilder setIssuerId(String issuerId) {
        filterValues.put(CLI_OKTA_ISSUER_ID, issuerId);
        return this;
    }

    public FilterConfigBuilder setIssuer(String issuer) {
        filterValues.put(CLI_OKTA_ISSUER, issuer);
        setOrgUrl(URI.create(issuer).resolve("/").toString());
        return this;
    }

    public FilterConfigBuilder setOrgUrl(String orgUrl) {
        filterValues.put(CLI_OKTA_ORG_URL, orgUrl);

        String[] hostParts = URI.create(orgUrl).getHost().split("\\.");
        String reverseDomain = IntStream.rangeClosed(1, hostParts.length)
                    .mapToObj(i -> hostParts[hostParts.length - i])
                    .collect(Collectors.joining("."));
        setReverseDomain(reverseDomain);
        return this;
    }

    public FilterConfigBuilder setReverseDomain(String reverseDomain) {
        filterValues.put(CLI_OKTA_REVERSE_DOMAIN, reverseDomain);
        return this;
    }

    // TODO: this is a bit ugly...
    public FilterConfigBuilder fromPropertySource(MutablePropertySource propertySource) {
        return this.setIssuer(propertySource.getProperty("okta.oauth2.issuer"))
                .setClientId(propertySource.getProperty("okta.oauth2.client-id"))
                .setClientSecret(propertySource.getProperty("okta.oauth2.client-secret"));
    }

    public Map<String, String> build() {
        return Collections.unmodifiableMap(filterValues);
    }
}
