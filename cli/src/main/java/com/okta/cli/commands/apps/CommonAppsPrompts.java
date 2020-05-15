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
package com.okta.cli.commands.apps;

import com.okta.cli.common.model.AuthorizationServer;
import com.okta.cli.common.service.DefaultAuthorizationServerService;
import com.okta.cli.console.PromptOption;
import com.okta.cli.console.Prompter;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class CommonAppsPrompts {

    private static final String DEFAULT_ISSUER_NAME = "default";

    private CommonAppsPrompts() {}

    static AuthorizationServer getIssuer(Client client, Prompter prompter, String authorizationServerId) {
        Map<String, AuthorizationServer> asMap = new DefaultAuthorizationServerService().authorizationServersMap(client);

        if (!Strings.isEmpty(authorizationServerId)) {
            AuthorizationServer as = asMap.get(authorizationServerId);
            if (as == null) {
                throw new IllegalArgumentException("The authorization-server-id specified was not found");
            } else {
                return as;
            }
        } else if (asMap.isEmpty()) {
            throw new IllegalArgumentException("No custom authorization servers were found in this Okta org, create one in the Okta Admin Console and try again");
        } else if (asMap.size() == 1) {
            return asMap.values().iterator().next();
        } else if (asMap.containsKey(DEFAULT_ISSUER_NAME)) {
            return asMap.get(DEFAULT_ISSUER_NAME);
        } else {
            List<PromptOption<AuthorizationServer>> issuerOptions = asMap.values().stream()
                    .map(it -> PromptOption.of(it.getIssuer(), it))
                    .collect(Collectors.toList());

            return prompter.prompt("Issuer:", issuerOptions, issuerOptions.get(0));
        }
    }
}
