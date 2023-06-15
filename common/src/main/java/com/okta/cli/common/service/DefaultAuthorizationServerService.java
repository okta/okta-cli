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

import com.okta.cli.common.model.AuthorizationServer;
import com.okta.cli.common.model.AuthorizationServerList;
import com.okta.commons.lang.Assert;
import com.okta.sdk.client.Client;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.authorization.server.AuthorizationServerPolicy;
import com.okta.sdk.resource.authorization.server.AuthorizationServerPolicyList;
import com.okta.sdk.resource.authorization.server.policy.AuthorizationServerPolicyRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultAuthorizationServerService implements AuthorizationServerService {

    @Override
    public Map<String, AuthorizationServer> authorizationServersMap(Client client) {
        AuthorizationServerList asList = client.http().get("/api/v1/authorizationServers", AuthorizationServerList.class);
        return asList.stream()
                .collect(Collectors.toMap(as -> (String) as.get("id"), as -> as));
    }

    private boolean containsGroupClaim(Client client, String groupClaimName, String authorizationServerId) {
        Assert.hasText(groupClaimName, "Group claim name cannot be empty");
        ExtensibleResource claims = client.http()
                .get("/api/v1/authorizationServers/" + authorizationServerId + "/claims", ExtensibleResource.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) claims.get("items");

        return items.stream()
                .anyMatch(it -> groupClaimName.equals(it.get("name")));
    }

    @Override
    public void createGroupClaim(Client client, String groupClaimName, String authorizationServerId) {

        // first check if the claim exists
        if (!containsGroupClaim(client, groupClaimName, authorizationServerId)) {
            // create the group claim
            createClaim(client, groupClaimName, authorizationServerId, "RESOURCE");
            createClaim(client, groupClaimName, authorizationServerId, "IDENTITY");
        }
    }

    @Override
    public Optional<AuthorizationServerPolicyRule> getSinglePolicyRule(Client client, String authorizationServerId) {

        List<AuthorizationServerPolicy> policies = client.http()
                .get("/api/v1/authorizationServers/" + authorizationServerId + "/policies", AuthorizationServerPolicyList.class)
                .stream()
                .toList();

        if (policies.size() != 1) {
            return Optional.empty();
        }

        List<AuthorizationServerPolicyRule> rules = policies.get(0).listPolicyRules(authorizationServerId).stream().toList();
        if (rules.size() != 1) {
            return Optional.empty();
        }

        return Optional.of(rules.get(0));
    }

    @Override
    public void enableDeviceGrant(Client client, String authorizationServerId, AuthorizationServerPolicyRule rule) {
        List<String> grantTypes = rule.getConditions().getGrantTypes().getInclude();
        if (!grantTypes.contains(SetupService.APP_TYPE_DEVICE)) {
            grantTypes.add(SetupService.APP_TYPE_DEVICE);
            rule.update(authorizationServerId);
        }
    }

    private void createClaim(Client client, String groupClaimName, String authorizationServerId, String claimType) {
        ExtensibleResource claimResource = client.instantiate(ExtensibleResource.class);
        claimResource.put("name", groupClaimName);
        claimResource.put("status", "ACTIVE");
        claimResource.put("claimType", claimType);
        claimResource.put("valueType", "GROUPS");
        claimResource.put("value", ".*");
        claimResource.put("alwaysIncludeInToken", true);
        claimResource.put("group_filter_type", "REGEX");
        ExtensibleResource conditions = client.instantiate(ExtensibleResource.class);
        conditions.put("scopes", Collections.emptyList());
        claimResource.put("conditions", conditions);

        client.http()
                .setBody(claimResource)
                .post("/api/v1/authorizationServers/" + authorizationServerId + "/claims", ExtensibleResource.class);
    }
}
