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

import com.okta.cli.common.RestException;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.cli.common.model.UserProfileRequestWrapper;

import java.io.IOException;

public class DefaultOktaOrganizationCreator implements OktaOrganizationCreator {

    private final RestClient restClient = new DefaultStartRestClient(Settings.getRegistrationBaseUrl());

    private final String registrationId = Settings.getRegistrationId();

    @Override
    public OrganizationResponse createNewOrg(OrganizationRequest orgRequest) throws RestException, IOException {
        return restClient.post("/api/v1/registration/" + registrationId + "/register", new UserProfileRequestWrapper(orgRequest), OrganizationResponse.class);
    }

    @Override
    public OrganizationResponse verifyNewOrg(String identifier) throws RestException, IOException {
        return restClient.get("/api/internal/v1/developer/redeem/" + identifier, OrganizationResponse.class);
    }

}