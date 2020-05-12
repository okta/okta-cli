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
import com.okta.cli.commands.apps.templates.AppType;
import com.okta.cli.commands.apps.templates.ServiceAppTemplate;
import com.okta.cli.commands.apps.templates.WebAppTemplate;
import com.okta.cli.common.config.MapPropertySource;
import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ClientConfigurationException;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.PromptOption;
import com.okta.cli.console.Prompter;
import com.okta.cli.util.InternalApiUtil;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CommandLine.Command(name = "create",
        description = "Create an new Okta app")
public class AppsCreate implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @CommandLine.Mixin
    private AppCreationMixin appCreationMixin;

    @CommandLine.Parameters(hidden = true, converter = EnumTypeConverter.class)
    List<QuickTemplate> quickTemplates;

    @Override
    public Integer call() throws Exception {

        if (quickTemplates != null && quickTemplates.size() > 1) {
            throw new IllegalArgumentException("Only one positional parameter is allowed");
        }

        QuickTemplate quickTemplate = quickTemplates == null
                ? null
                : quickTemplates.get(0);

        Prompter prompter = standardOptions.getEnvironment().prompter();

        AppType appType;
        Object appTemplate = null;
        if (quickTemplate == null) {
            appType = prompter.prompt("Type of Application\n(The Okta CLI only supports a subset of application types and properties):", Arrays.asList(AppType.values()), AppType.WEB);
        } else {
            appType = quickTemplate.appType;
            appTemplate = quickTemplate.appTemplate;
        }

        switch(appType) {
            case WEB:
                return createWebApp((WebAppTemplate) appTemplate);
            case SPA:
                return createSpaApp();
            case NATIVE:
                return createNativeApp();
            case SERVICE:
                return createServiceApp((ServiceAppTemplate) appTemplate);
            default:
                throw new IllegalStateException("Unsupported AppType: "+ appType);
        }
    }

    private int createWebApp(WebAppTemplate webAppTemplate) throws IOException {

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();
        Prompter prompter = standardOptions.getEnvironment().prompter();
        String appName = getAppName();

        WebAppTemplate appTemplate = prompter.promptIfEmpty(webAppTemplate, "Type of Application", Arrays.asList(WebAppTemplate.values()), WebAppTemplate.GENERIC);

        String redirectUri = getRedirectUri(Map.of("Spring Security", "http://localhost:8080/login/oauth2/code/okta",
                                                   "JHipster", "http://localhost:8080/login/oauth2/code/oidc"),
                                            appTemplate.getDefaultRedirectUri());

        Client client = Clients.builder().build();

        Map<String, Object> issuer = getIssuer(client);

        String baseUrl = getBaseUrl();

        String groupClaimName = appTemplate.getGroupsClaim();

        MutablePropertySource propertySource = appCreationMixin.getPropertySource(appTemplate.getDefaultConfigFileName());

        new DefaultSetupService(appTemplate.getSpringPropertyKey()).createOidcApplication(propertySource, appName, baseUrl, groupClaimName, (String) issuer.get("id"), true, OpenIdConnectApplicationType.WEB, redirectUri);

        out.writeLine("Okta application configuration has been written to: " + propertySource.getName());

        return 0;
    }

    private Integer createNativeApp() throws IOException {

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();
        String appName = getAppName();

        String baseUrl = getBaseUrl();

        String[] parts = URI.create(baseUrl).getHost().split("\\.");
        String reverseDomain = IntStream.rangeClosed(1, parts.length)
                .mapToObj(i -> parts[parts.length - i])
                .collect(Collectors.joining("."));

        String defaultRedirectUri = reverseDomain + ":/callback";

        String redirectUri = getRedirectUri(Map.of("Reverse Domain name", "com.example:/callback"), defaultRedirectUri);

        Client client = Clients.builder().build();

        Map<String, Object> issuer = getIssuer(client);

        MutablePropertySource propertySource = new MapPropertySource();

        new DefaultSetupService(null).createOidcApplication(propertySource, appName, baseUrl, null, (String) issuer.get("id"), true, OpenIdConnectApplicationType.NATIVE, redirectUri);

        out.writeLine("Okta application configuration: ");
        propertySource.getProperties().forEach((key, value) -> {
            out.bold(key);
            out.write(": ");
            out.writeLine(value);
        });

        return 0;
    }

    private Integer createServiceApp(ServiceAppTemplate appTemplate) throws IOException {

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();
        Prompter prompter = standardOptions.getEnvironment().prompter();

        appTemplate = prompter.promptIfEmpty(appTemplate, "Framework of Application", Arrays.asList(ServiceAppTemplate.values()), ServiceAppTemplate.SPRING_BOOT);

        String appName = getAppName();
        String baseUrl = getBaseUrl();
        Client client = Clients.builder().build();
        Map<String, Object> issuer = getIssuer(client);

        MutablePropertySource propertySource = appCreationMixin.getPropertySource(appTemplate.getDefaultConfigFileName());
        new DefaultSetupService(appTemplate.getSpringPropertyKey()).createOidcApplication(propertySource, appName, baseUrl, null, (String) issuer.get("id"), true, OpenIdConnectApplicationType.SERVICE);

        out.writeLine("Okta application configuration has been written to: " + propertySource.getName());

        return 0;
    }

    private Integer createSpaApp() throws IOException {

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();
        String appName = getAppName();

        String baseUrl = getBaseUrl();

        String redirectUri = getRedirectUri(Map.of("/callback", "http://localhost:8080/callback"), "http://localhost:8080/callback");

        Client client = Clients.builder().build();

        Map<String, Object> issuer = getIssuer(client);

        MutablePropertySource propertySource = new MapPropertySource();

        new DefaultSetupService(null).createOidcApplication(propertySource, appName, baseUrl, null, (String) issuer.get("id"), true, OpenIdConnectApplicationType.BROWSER, redirectUri);

        out.writeLine("Okta application configuration: ");
        propertySource.getProperties().forEach((key, value) -> {
            out.bold(key);
            out.write(": ");
            out.writeLine(value);
        });

        return 0;
    }

    private String getAppName() {
        Prompter prompter = standardOptions.getEnvironment().prompter();
        return prompter.promptUntilIfEmpty(appCreationMixin.appName,"Application name", appCreationMixin.getDefaultAppName());
    }

    private Map<String, Object> getIssuer(Client client) {
        Prompter prompter = standardOptions.getEnvironment().prompter();
        Map<String, Map<String, Object>> asMap = InternalApiUtil.getAuthorizationServers(client);

        if (!Strings.isEmpty(appCreationMixin.authorizationServerId)) {
            Map<String, Object> as = asMap.get(appCreationMixin.authorizationServerId);
            if (as == null) {
                throw new IllegalArgumentException("The authorization-server-id specified was not found");
            } else {
                return as;
            }
        } else if (asMap.isEmpty()) {
            throw new IllegalArgumentException("No custom authorization servers were found in this Okta org, create one in the Okta Admin Console and try again");
        } else if (asMap.size() == 1) {
            return asMap.values().iterator().next();
        } else if (asMap.containsKey("default")) {
            return asMap.get("default");
        } else {
            List<PromptOption<Map<String, Object>>> issuerOptions = asMap.values().stream()
                    .map(it -> PromptOption.of((String) it.get("issuer"), it))
                    .collect(Collectors.toList());

            return prompter.prompt("Issuer:", issuerOptions, issuerOptions.get(0));
        }
    }

    private String getBaseUrl() {
        try {
            // TODO more hacking
            return new DefaultSdkConfigurationService().loadUnvalidatedConfiguration().getBaseUrl();
        } catch (ClientConfigurationException e) {
            throw new IllegalStateException("Unable to find base URL, run `okta login` and try again");
        }
    }

    private String getRedirectUri(Map<String, String> commonExamples, String defaultRedirectUri) {
        Prompter prompter = standardOptions.getEnvironment().prompter();

        StringBuilder redirectUriPrompt = new StringBuilder("Redirect URI\nCommon defaults:\n");
        commonExamples.forEach((key, value) -> {
                redirectUriPrompt.append(" ").append(key).append(" - ").append(value).append("\n");
        });
       redirectUriPrompt.append("Enter your Redirect URI");

        return prompter.promptIfEmpty(appCreationMixin.redirectUri, redirectUriPrompt.toString(), defaultRedirectUri);
    }

    private enum QuickTemplate {
        // web
        SPRING_BOOT("spring-boot", AppType.WEB, WebAppTemplate.SPRING_BOOT),
        JHIPSTER("jhipster", AppType.WEB, WebAppTemplate.JHIPSTER),
        GENERIC_WEB("web", AppType.WEB, WebAppTemplate.GENERIC),
        // service
        SPRING_BOOT_SERVICE("spring-boot-service", AppType.SERVICE, ServiceAppTemplate.SPRING_BOOT),
        JHIPSTER_SERVICE("jhipster-service", AppType.SERVICE, ServiceAppTemplate.JHIPSTER),
        GENERIC_SERVICE("service", AppType.SERVICE, ServiceAppTemplate.GENERIC);

        private static final List<String> names = Arrays.stream(values()).map(it -> it.friendlyName).collect(Collectors.toList());

        private final String friendlyName;
        private final AppType appType;
        private final Object appTemplate; // TODO, this is ugly (needs a base type)

        QuickTemplate(String friendlyName, AppType appType, Object appTemplate) {
            this.friendlyName = friendlyName;
            this.appType = appType;
            this.appTemplate = appTemplate;
        }

        static QuickTemplate fromName(String name) {
            return Arrays.stream(values())
                    .filter(it -> it.friendlyName.equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("template must be empty or one of: " + names));
        }
    }

    static class EnumTypeConverter implements CommandLine.ITypeConverter<QuickTemplate> {

        @Override
        public QuickTemplate convert(String value) throws Exception {
            return QuickTemplate.fromName(value);
        }
    }
}
