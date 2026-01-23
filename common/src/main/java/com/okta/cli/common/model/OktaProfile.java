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

import java.util.Objects;

/**
 * Represents an Okta profile configuration containing org URL and API token.
 * Used for multi-profile support to manage multiple Okta organizations.
 */
public class OktaProfile {

    private final String name;
    private final String orgUrl;
    private final String apiToken;

    public OktaProfile(String name, String orgUrl, String apiToken) {
        this.name = Objects.requireNonNull(name, "Profile name cannot be null");
        this.orgUrl = Objects.requireNonNull(orgUrl, "Org URL cannot be null");
        this.apiToken = Objects.requireNonNull(apiToken, "API token cannot be null");
    }

    public String getName() {
        return name;
    }

    public String getOrgUrl() {
        return orgUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OktaProfile that = (OktaProfile) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "OktaProfile{" +
                "name='" + name + '\'' +
                ", orgUrl='" + orgUrl + '\'' +
                '}';
    }
}
