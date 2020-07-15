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
package com.okta.cli.common.model;

import java.util.Objects;

public class OrganizationResponse {

    private String orgUrl;
    private String email;
    private String apiToken;
    private String verifyUuid;
    private String passwordResetUrl;

    public String getOrgUrl() {
        return orgUrl;
    }

    public OrganizationResponse setOrgUrl(String orgUrl) {
        this.orgUrl = orgUrl;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public OrganizationResponse setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getApiToken() {
        return apiToken;
    }

    public OrganizationResponse setApiToken(String apiToken) {
        this.apiToken = apiToken;
        return this;
    }

    public String getVerifyUuid() {
        return verifyUuid;
    }

    public void setVerifyUuid(String verifyUuid) {
        this.verifyUuid = verifyUuid;
    }

    public String getPasswordResetUrl() {
        return passwordResetUrl;
    }

    public void setPasswordResetUrl(String passwordResetUrl) {
        this.passwordResetUrl = passwordResetUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationResponse that = (OrganizationResponse) o;
        return Objects.equals(orgUrl, that.orgUrl) &&
               Objects.equals(email, that.email) &&
               Objects.equals(apiToken, that.apiToken) &&
               Objects.equals(verifyUuid, that.verifyUuid) &&
               Objects.equals(passwordResetUrl, that.passwordResetUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgUrl, email, apiToken, verifyUuid, passwordResetUrl);
    }
}
