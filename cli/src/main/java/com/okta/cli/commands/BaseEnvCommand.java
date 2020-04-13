package com.okta.cli.commands;

import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

abstract class BaseEnvCommand extends BaseCommand {

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = "--application-config-file", description = "Spring application config file, will search for `src/main/resources/application.yaml|properties")
    protected File applicationConfigFile = null;

    @CommandLine.Option(names = "--oidc-application-name", description = "OIDC Application name to be created, defaults to current directory name")
    protected String oidcAppName = new File(System.getProperty("user.dir")).getName();

    @CommandLine.Option(names = "--authorization-server-id", description = "Okta Authorization Server Id", defaultValue = "default")
    protected String authorizationServerId = "default";

    protected String[] springBasedRedirectUris(String key) {
        return new String[] {"http://localhost:8080/authorization-code/callback", // Okta Dev console defaults
                             "http://localhost:8080/login/oauth2/code/" + key};
    }

    @Override
    CommandLine.Model.CommandSpec getCommandSpec() {
        return spec;
    }
}
