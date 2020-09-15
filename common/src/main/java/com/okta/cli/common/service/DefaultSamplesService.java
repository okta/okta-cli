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

import com.okta.cli.common.RestException;
import com.okta.cli.common.model.SamplesListings;

import java.io.IOException;
import java.util.List;

public class DefaultSamplesService implements SamplesService {

    private final RestClient restClient = new HttpRestClient();

    @Override
    public List<SamplesListings.OktaSample> listSamples() throws IOException, RestException {
        return restClient.get("/samples", SamplesListings.class).getItems();
    }
}
