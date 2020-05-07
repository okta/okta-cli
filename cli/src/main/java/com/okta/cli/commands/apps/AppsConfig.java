package com.okta.cli.commands.apps;

import com.okta.cli.OktaCli;
import com.okta.cli.console.ConsoleOutput;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.ExtensibleResource;
import com.okta.sdk.resource.application.Application;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "config",
        description = "Show an Okta app's configuration")
public class AppsConfig implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @CommandLine.Option(names = "--app", description = "App ID", required = true)
    private String appName;

    @Override
    public Integer call() throws Exception {
        Client client = Clients.builder().build();
        Application app = client.getApplication(appName);

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();
        
        // TODO verify this is an OIDC app

        ExtensibleResource clientCreds = client.http()
                .get("/api/v1/internal/apps/" + app.getId() + "/settings/clientcreds", ExtensibleResource.class);

        out.writeLine("Name:          " + app.getLabel());

        out.writeLine("Client Id:     " + clientCreds.getString("client_id"));
        if (clientCreds.containsKey("client_secret")) {
            out.writeLine("Client Secret: " + clientCreds.getString("client_secret"));
        }

        List<Map<String, Object>> authServers = (List<Map<String, Object>>) client.http().get("/api/v1/authorizationServers", ExtensibleResource.class).get("items");

        Map<String, Map<String, Object>> asMap = authServers.stream()
                .collect(Collectors.toMap(as -> (String) as.get("id"), as -> as));

        if (asMap.size() == 1) {
            out.writeLine("Issuer:        "+ asMap.keySet().iterator().next());
        } else if (asMap.containsKey("default")) {
            out.writeLine("Issuer:        "+ asMap.get("default").get("issuer"));
        } else {
            out.writeLine("Issuers:");
            asMap.values().forEach(as -> {
                out.writeLine("  " +as.get("name") + "\t" + as.get("issuer"));
            });
        }

        return 0;
    }
}
