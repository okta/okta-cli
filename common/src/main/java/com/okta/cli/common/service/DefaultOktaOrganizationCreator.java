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

import com.okta.cli.common.FactorVerificationException;
import com.okta.cli.common.RestException;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;

import java.io.IOException;

public class DefaultOktaOrganizationCreator implements OktaOrganizationCreator {

    private final RestClient restClient = new HttpRestClient();

    @Override
    public OrganizationResponse createNewOrg(OrganizationRequest orgRequest) throws RestException, IOException {

        return restClient.post("/create", orgRequest, OrganizationResponse.class);
    }

    @Override
    public OrganizationResponse verifyNewOrg(String identifier, String code) throws FactorVerificationException, IOException {

        String postBody = "{\"code\":\"" + code + "\"}";

        try {
            return restClient.post("/verify/" + identifier, postBody, OrganizationResponse.class);
        } catch (RestException e) {
            throw new FactorVerificationException(e.getErrorResponse(), e);
        }
    }
}