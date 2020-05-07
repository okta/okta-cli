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
import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.Prompter;
import com.okta.cli.util.InternalApiUtil;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import picocli.CommandLine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "create",
        description = "Create an new Okta app")
public class AppsCreate implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @CommandLine.Mixin
    private AppCreationMixin appCreationMixin;

    @CommandLine.Parameters(hidden = true, converter = EnumTypeConverter.class)
    List<AppTemplate> appTemplates;

    @Override
    public Integer call() throws Exception {

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();

        if (appTemplates != null && appTemplates.size() > 1) {
            throw new IllegalArgumentException("Only one positional parameter is allowed");
        }

        AppTemplate appTemplate = appTemplates == null
                ? AppTemplate.GENERIC_WEB
                : appTemplates.get(0);

        Prompter prompter = standardOptions.getEnvironment().prompter();

        Map<String, String> appTypes = Map.of("web", "Web",
                                              "spa", "Single Page App",
                                              "native", "Native App (mobile)");

        String appType = prompter.prompt("Type of Application\n(The Okta CLI only supports a subset of application types and properties):", appTypes, "web"); // TODO everything below assumes "web"

        String appName = prompter.promptIfEmpty(appCreationMixin.appName,"Application name", appCreationMixin.getDefaultAppName());

        String redirectUriPrompt = "Redirect URI\n" +
                                   "Common defaults:\n" +
                                   " Spring Security - http://localhost:8080/login/oauth2/code/okta\n" +
                                   " JHipster -        http://localhost:8080/login/oauth2/code/oidc\n" +
                                   "\n" +
                                   "Enter your Redirect URI";

        String redirectUri = prompter.promptIfEmpty(appCreationMixin.redirectUri, redirectUriPrompt, appTemplate.defaultRedirectUri);

        Client client = Clients.builder().build();

        Map<String, Map<String, Object>> asMap = InternalApiUtil.getAuthorizationServers(client);

        String issuer;
        if (!Strings.isEmpty(appCreationMixin.authorizationServerId)) {
            Map<String, Object> as = asMap.get(appCreationMixin.authorizationServerId);
            if (as == null) {
                throw new IllegalArgumentException("The authorization-server-id specified was not found");
            } else {
                issuer = (String) as.get("issuer");
            }
        } else if (asMap.isEmpty()) {
            throw new IllegalArgumentException("No custom authorization servers were found in this Okta org, create one in the Okta Admin Console and try again");
        } else if (asMap.size() == 1) {
            issuer = asMap.keySet().iterator().next();
        } else if (asMap.containsKey("default")) {
            issuer = (String) asMap.get("default").get("issuer");
        } else {
            issuer = prompter.prompt("Issuer:", new ArrayList<>(asMap.keySet()), 0);
        }

        // TODO more hacking
        String baseUrl = new DefaultSdkConfigurationService().loadUnvalidatedConfiguration().getBaseUrl();

        String groupClaimName = appTemplate.groupsClaim;

        MutablePropertySource propertySource = appCreationMixin.getPropertySource(appTemplate.defaultConfigFileName);

        new DefaultSetupService(appTemplate.springPropertyKey).createOidcApplication(propertySource, appName, baseUrl, groupClaimName, (String) asMap.get(issuer).get("id"), true, redirectUri);

        out.writeLine("This configuration has been written to: " + propertySource.getName());

        return 0;
    }

    private enum AppTemplate {
        SPRING_BOOT("spring-boot", "okta", "src/main/resources/application.properties", "http://localhost:8080/login/oauth2/code/okta", null),
        JHIPSTER("jhipster", "oidc", ".okta.env", "http://localhost:8080/login/oauth2/code/oidc", "groups"),
        GENERIC_WEB("web", null, ".okta.env", "http://localhost:8080/authorization-code/callback", null);

        private static final List<String> names = Arrays.stream(AppTemplate.values()).map(it -> it.friendlyName).collect(Collectors.toList());

        private final String friendlyName;
        private final String springPropertyKey;
        private final String defaultConfigFileName;
        private final String defaultRedirectUri;
        private final String groupsClaim;

        AppTemplate(String friendlyName, String springPropertyKey, String defaultConfigFileName, String defaultRedirectUri, String groupsClaim) {
            this.friendlyName = friendlyName;
            this.springPropertyKey = springPropertyKey;
            this.defaultConfigFileName = defaultConfigFileName;
            this.defaultRedirectUri = defaultRedirectUri;
            this.groupsClaim = groupsClaim;
        }

        static AppTemplate fromName(String name) {
            return Arrays.stream(AppTemplate.values())
                    .filter(it -> it.friendlyName.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("template must be empty or one of: " + names));
        }
    }

    static class EnumTypeConverter implements CommandLine.ITypeConverter<AppTemplate> {

        @Override
        public AppTemplate convert(String value) throws Exception {
            return AppTemplate.fromName(value);
        }
    }

    static class Timer {
        private long start = System.currentTimeMillis();

        public static Timer start() {
            return new Timer();
        }

        public Duration mark() {
            long now = System.currentTimeMillis();
            Duration result = Duration.ofMillis(now - start);
            start = System.currentTimeMillis();
            return result;
        }

        public void printMark() {
            System.out.println(mark());
        }
    }
}
