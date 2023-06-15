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

import com.okta.cli.commands.BaseCommand;
import com.okta.cli.commands.apps.templates.*;
import com.okta.cli.common.URIs;
import com.okta.cli.common.config.MapPropertySource;
import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.AuthorizationServer;
import com.okta.cli.common.model.OidcProperties;
import com.okta.cli.common.service.ClientConfigurationException;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.Prompter;
import com.okta.commons.lang.Assert;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(name = "create",
        description = "Create a new Okta app")
public class AppsCreate extends BaseCommand {

    private static final String NO_BASE_URL_ERROR = "Unable to find base URL, run `okta login` and try again";

    @CommandLine.Mixin
    private AppCreationMixin appCreationMixin;

    @CommandLine.Parameters(hidden = true, converter = EnumTypeConverter.class)
    List<QuickTemplate> quickTemplates;

    @Override
    public int runCommand() throws Exception {

        if (quickTemplates != null && quickTemplates.size() > 1) {
            throw new IllegalArgumentException("Only one positional parameter is allowed");
        }

        QuickTemplate quickTemplate = quickTemplates == null
                ? null
                : quickTemplates.get(0);

        Prompter prompter = getPrompter();

        // prompt (if needed) for the applicaiton name first
        String appName = getAppName();

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
                return createWebApp(appName, (WebAppTemplate) appTemplate);
            case SPA:
                return createSpaApp(appName);
            case NATIVE:
                return createNativeApp(appName);
            case DEVICE:
                return createDeviceApp(appName);
            case SERVICE:
                return createServiceApp(appName,(ServiceAppTemplate) appTemplate);
            default:
                throw new IllegalStateException("Unsupported AppType: "+ appType);
        }
    }

    private int createWebApp(String appName, WebAppTemplate webAppTemplate) throws IOException {

        ConsoleOutput out = getConsoleOutput();
        Prompter prompter = getPrompter();

        WebAppTemplate appTemplate = prompter.promptIfEmpty(webAppTemplate, "Framework of Application", WebAppTemplate.values(), WebAppTemplate.GENERIC);

        List<String> redirectUris = getRedirectUris(Map.of("Spring Security", "http://localhost:8080/login/oauth2/code/okta",
                                                    "Quarkus OIDC", "http://localhost:8080/callback",
                                                    "JHipster", "http://localhost:8080/login/oauth2/code/oidc"),
                                            appTemplate.getDefaultRedirectUris());
        List<String> postLogoutRedirectUris = getPostLogoutRedirectUris(redirectUris, appTemplate.getDefaultPostLogoutEndpoint());
        Client client = Clients.builder().build();
        AuthorizationServer issuer = getIssuer(client);
        String baseUrl = getBaseUrl();
        String groupClaimName = appTemplate.getGroupsClaim();
        Set<String> groupsToCreate = appTemplate.getGroupsToCreate();

        OidcProperties oidcProperties = appTemplate.getOidcProperties();

        MutablePropertySource propertySource = appCreationMixin.getPropertySource(appTemplate.getDefaultConfigFileName());
        new DefaultSetupService(oidcProperties).createOidcApplication(propertySource, appName, baseUrl, groupClaimName, groupsToCreate, issuer.getIssuer(), issuer.getId(), true, OpenIdConnectApplicationType.WEB.toString(), redirectUris, postLogoutRedirectUris, client);

        out.writeLine("Okta application configuration has been written to: " + propertySource.getName());

        return 0;
    }

    private Integer createNativeApp(String appName) throws IOException {

        ConsoleOutput out = getConsoleOutput();
        String baseUrl = getBaseUrl();
        String reverseDomain = URIs.reverseDomain(baseUrl);

        String defaultRedirectUri = reverseDomain + ":/callback";
        List<String> redirectUris = getRedirectUris(Map.of("Reverse Domain name", defaultRedirectUri), defaultRedirectUri);
        List<String> postLogoutRedirectUris = getPostLogoutRedirectUris(redirectUris);
        Client client = Clients.builder().build();
        AuthorizationServer issuer = getIssuer(client);

        MutablePropertySource propertySource = new MapPropertySource();
        new DefaultSetupService(OidcProperties.oktaEnv()).createOidcApplication(propertySource, appName, baseUrl, null, Collections.emptySet(), issuer.getIssuer(), issuer.getId(), getEnvironment().isInteractive(), OpenIdConnectApplicationType.NATIVE.toString(), redirectUris, postLogoutRedirectUris, client);

        out.writeLine("Okta application configuration: ");
        propertySource.getProperties().forEach((key, value) -> {
            out.bold(key);
            out.write(": ");
            out.writeLine(value);
        });

        return 0;
    }

    private Integer createDeviceApp(String appName) throws IOException {

        ConsoleOutput out = getConsoleOutput();
        String baseUrl = getBaseUrl();

        Client client = Clients.builder().build();
        AuthorizationServer issuer = getIssuer(client);

        MutablePropertySource propertySource = new MapPropertySource();
        new DefaultSetupService(OidcProperties.oktaEnv()).createOidcApplication(propertySource, appName, baseUrl, null, Collections.emptySet(), issuer.getIssuer(), issuer.getId(), getEnvironment().isInteractive(), SetupService.APP_TYPE_DEVICE, client);

        out.writeLine("Okta application configuration: ");
        propertySource.getProperties().forEach((key, value) -> {
            out.bold(key);
            out.write(": ");
            out.writeLine(value);
        });

        return 0;
    }

    private Integer createServiceApp(String appName, ServiceAppTemplate appTemplate) throws IOException {

        ConsoleOutput out = getConsoleOutput();
        Prompter prompter = getPrompter();

        appTemplate = prompter.promptIfEmpty(appTemplate, "Framework of Application", ServiceAppTemplate.values(), ServiceAppTemplate.GENERIC);

        String baseUrl = getBaseUrl();
        Client client = Clients.builder().build();
        AuthorizationServer issuer = getIssuer(client);

        MutablePropertySource propertySource = appCreationMixin.getPropertySource(appTemplate.getDefaultConfigFileName());
        new DefaultSetupService(appTemplate.getOidcProperties()).createOidcApplication(propertySource, appName, baseUrl, null, Collections.emptySet(), issuer.getIssuer(), issuer.getId(), getEnvironment().isInteractive(), OpenIdConnectApplicationType.SERVICE.toString(), client);

        out.writeLine("Okta application configuration has been written to: " + propertySource.getName());

        return 0;
    }

    private Integer createSpaApp(String appName) throws IOException {

        ConsoleOutput out = getConsoleOutput();

        String baseUrl = getBaseUrl();
        List<String> redirectUris = getRedirectUris(Map.of("/callback", "http://localhost:8080/callback"), SpaAppTemplate.GENERIC.getDefaultRedirectUri());
        List<String> postLogoutRedirectUris = getPostLogoutRedirectUris(redirectUris);
        Client client = Clients.builder().build();
        AuthorizationServer authorizationServer = getIssuer(client);
        List<String> trustedOrigins = redirectUris.stream().map(URIs::baseUrlOf).collect(Collectors.toList());

        MutablePropertySource propertySource = new MapPropertySource();
        new DefaultSetupService(OidcProperties.oktaEnv()).createOidcApplication(propertySource, appName, baseUrl, null, Collections.emptySet(), authorizationServer.getIssuer(), authorizationServer.getId(), getEnvironment().isInteractive(), OpenIdConnectApplicationType.BROWSER.toString(), redirectUris, postLogoutRedirectUris, trustedOrigins, client);

        out.writeLine("Okta application configuration: ");
        out.bold("Issuer:    ");
        out.writeLine(propertySource.getProperty("okta.oauth2.issuer"));
        out.bold("Client ID: ");
        out.writeLine(propertySource.getProperty("okta.oauth2.client-id"));
        return 0;
    }

    private String getAppName() {
        Prompter prompter = getPrompter();
        return prompter.promptUntilIfEmpty(appCreationMixin.appName,"Application name", appCreationMixin.getDefaultAppName());
    }

    private AuthorizationServer getIssuer(Client client) {
        Prompter prompter = getPrompter();
        return CommonAppsPrompts.getIssuer(client, prompter, appCreationMixin.authorizationServerId);
    }

    private String getBaseUrl() {
        try {
            String baseUrl = new DefaultSdkConfigurationService().loadUnvalidatedConfiguration().getBaseUrl();
            if (Strings.isEmpty(baseUrl)) {
                throw new IllegalStateException(NO_BASE_URL_ERROR);
            }
            return baseUrl;
        } catch (ClientConfigurationException e) {
            throw new IllegalStateException(NO_BASE_URL_ERROR, e);
        }
    }

    private List<String> getRedirectUris(Map<String, String> commonExamples, List<String> defaultRedirectUris) {
        Prompter prompter = getPrompter();

        StringBuilder redirectUriPrompt = new StringBuilder("Redirect URI\nCommon defaults:\n");
        commonExamples.forEach((key, value) -> {
            redirectUriPrompt.append(" ").append(key).append(" - ").append(value).append("\n");
        });
        redirectUriPrompt.append("Enter your Redirect URI(s)");

        String redirectUrisString = String.join(", ", defaultRedirectUris);
        String result = prompter.promptIfEmpty(appCreationMixin.redirectUri, redirectUriPrompt.toString(), redirectUrisString).trim();
        return split(result);
    }

    private List<String> getRedirectUris(Map<String, String> commonExamples, String defaultRedirectUri) {
        return getRedirectUris(commonExamples, Collections.singletonList(defaultRedirectUri));
    }

    private List<String> getPostLogoutRedirectUris(List<String> redirectUris) {
        return getPostLogoutRedirectUris(redirectUris, "/");
    }

    private List<String> getPostLogoutRedirectUris(List<String> redirectUris, String defaultPostLogoutUri) {
        Prompter prompter = getPrompter();

        Assert.notEmpty(redirectUris, "Redirect Uris cannot be empty");
        String defaultPostLogoutUris = redirectUris.stream()
                .map(uri -> URIs.resolveUrl(uri, defaultPostLogoutUri))
                .collect(Collectors.joining(", "));

        String result = prompter.promptIfEmpty(appCreationMixin.redirectUri, "Enter your Post Logout Redirect URI(s)", defaultPostLogoutUris).trim();
        return split(result);
    }

    private List<String> split(String input) {
        String result = input.replaceFirst("^\\[", "");
        result = result.replaceFirst("]$", "");

        return Arrays.stream(result.split(","))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Quick templates are meant to reduce prompts for the end user, for example you could instruct a user to run
     * {@code okta apps create spring-boot-service} and they would be minimally prompted.
     */
    private enum QuickTemplate {
        // web
        OKTA_SPRING_BOOT("okta-spring-boot", AppType.WEB, WebAppTemplate.OKTA_SPRING_BOOT),
        SPRING_BOOT("spring-boot", AppType.WEB, WebAppTemplate.SPRING_BOOT),
        QUARKUS("quarkus", AppType.WEB, WebAppTemplate.QUARKUS),
        JHIPSTER("jhipster", AppType.WEB, WebAppTemplate.JHIPSTER),
        GENERIC_WEB("web", AppType.WEB, WebAppTemplate.GENERIC),
        SPA("spa", AppType.SPA, SpaAppTemplate.GENERIC),
        // native
        NATIVE("native", AppType.NATIVE, NativeAppTemplate.GENERIC),
        // CLI, TV, other device
        DEVICE("device", AppType.DEVICE, DeviceAppTemplate.GENERIC),
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
