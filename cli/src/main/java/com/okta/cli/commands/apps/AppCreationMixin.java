package com.okta.cli.commands.apps;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ConfigFileLocatorService;
import com.okta.commons.lang.Strings;
import picocli.CommandLine;

import java.io.File;

public class AppCreationMixin {

    @CommandLine.Option(names = "--config-file", description = {"Application config file, will search for:", " - src/main/resources/application.yml", " - src/main/resources/application.properties", " - .okta.env" })
    public File configFile;

    @CommandLine.Option(names = "--app-name", description = "Application name to be created, defaults to current directory name")
    public String appName;

    @CommandLine.Option(names = "--authorization-server-id", description = "Okta Authorization Server Id")
    public String authorizationServerId;

    @CommandLine.Option(names = "--redirect-uri", description = "OIDC Redirect URI")
    public String redirectUri;

    public String[] springBasedRedirectUris(String key) {

        if (redirectUri != null) {
            return new String[] { redirectUri };
        }

        return new String[] {"http://localhost:8080/authorization-code/callback", // Okta Dev console defaults
                "http://localhost:8080/login/oauth2/code/" + key};
    }

    public MutablePropertySource getPropertySource() {
        File currentDir = new File("").getAbsoluteFile();
        return new ConfigFileLocatorService().findApplicationConfig(currentDir, configFile);
    }

    public MutablePropertySource getPropertySource(String defaultConfigFile) {
        File currentDir = new File("").getAbsoluteFile();
        File appConfigFile = configFile != null ? configFile : new File(currentDir, defaultConfigFile);
        return new ConfigFileLocatorService().findApplicationConfig(currentDir, appConfigFile);
    }

    public String getOrDefaultAppName() {
        if (!Strings.isEmpty(appName)) {
            return appName;
        }
        return getDefaultAppName();
    }

    public String getDefaultAppName() {
        return new File(System.getProperty("user.dir")).getName();
    }
}
