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

import com.okta.cli.OktaCli;
import com.okta.cli.common.model.AuthorizationServer;
import com.okta.cli.common.model.ClientCredentials;
import com.okta.cli.console.ConsoleOutput;
import com.okta.commons.lang.Assert;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.application.Application;
import com.okta.sdk.resource.application.OpenIdConnectApplication;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "config",
        description = "Show an Okta app's configuration")
public class AppsConfig implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @CommandLine.Option(names = "--app", description = "App ID", required = true)
    private String appName;

    @Override
    public Integer call() {
        Client client = Clients.builder().build();
        Application app = client.getApplication(appName);

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();

        Assert.isInstanceOf(OpenIdConnectApplication.class, app, "Existing application found with name '" +
                appName +"' but it is NOT an OIDC application. Only OIDC applications work with the Okta CLI.");

        ClientCredentials clientCreds = new ClientCredentials(client.http()
                .get("/api/v1/internal/apps/" + app.getId() + "/settings/clientcreds", ExtensibleResource.class));

        AuthorizationServer authorizationServer = CommonAppsPrompts.getIssuer(client, standardOptions.getEnvironment().prompter(), null);

        out.writeLine("Name:          " + app.getLabel());
        out.writeLine("Client Id:     " + clientCreds.getClientId());
        if (clientCreds.hasClientSecret()) {
            out.writeLine("Client Secret: " + clientCreds.getClientSecret());
        }
        out.writeLine("Issuer:        " + authorizationServer.getIssuer());

        return 0;
    }
}
