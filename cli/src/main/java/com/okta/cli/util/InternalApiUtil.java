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
package com.okta.cli.util;

import com.okta.sdk.client.Client;
import com.okta.sdk.resource.ExtensibleResource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final public class InternalApiUtil {

    private InternalApiUtil() {}

    public static Map<String, Map<String, Object>> getAuthorizationServers(Client client) {
        ExtensibleResource resource = client.http().get("/api/v1/authorizationServers", ExtensibleResource.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> authServers = (List<Map<String, Object>>) resource.get("items");

        return authServers.stream()
                .collect(Collectors.toMap(as -> (String) as.get("id"), as -> as));
    }
}
