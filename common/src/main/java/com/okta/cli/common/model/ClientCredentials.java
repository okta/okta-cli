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

import com.okta.sdk.resource.ExtensibleResource;

public class ClientCredentials {

    private final ExtensibleResource resource;

    public ClientCredentials(ExtensibleResource resource) {
        this.resource = resource;
    }

    public String getClientId() {
        return resource.getString("client_id");
    }

    public String getClientSecret() {
        return resource.getString("client_secret");
    }

    public boolean hasClientSecret() {
        return resource.containsKey("client_secret");
    }
}
