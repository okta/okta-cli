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

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ConfigFileLocatorService;
import picocli.CommandLine;

import java.io.File;

public class AppCreationMixin {

    @CommandLine.Option(names = "--config-file", description = {"Application config file, will search for:", " - src/main/resources/application.yml", " - src/main/resources/application.properties", " - .okta.env" })
    public File configFile;

    @CommandLine.Option(names = "--app-name", description = "Application name to be created, defaults to current directory name.")
    public String appName;

    @CommandLine.Option(names = "--authorization-server-id", description = "Okta Authorization Server Id.")
    public String authorizationServerId;

    @CommandLine.Option(names = "--redirect-uri", description = "OIDC Redirect URI.")
    public String redirectUri;

    public MutablePropertySource getPropertySource(String defaultConfigFile) {
        File currentDir = new File("").getAbsoluteFile();
        File appConfigFile = configFile != null ? configFile : new File(currentDir, defaultConfigFile);
        return new ConfigFileLocatorService().findApplicationConfig(currentDir, appConfigFile);
    }

    public String getDefaultAppName() {
        return new File(System.getProperty("user.dir")).getName();
    }
}
