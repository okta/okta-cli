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
