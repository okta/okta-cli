package com.okta.cli.commands.apps.templates;

import com.okta.cli.console.PromptOption;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum WebAppTemplate implements PromptOption<WebAppTemplate> {

    SPRING_BOOT("spring-boot", "okta", "src/main/resources/application.properties", "http://localhost:8080/login/oauth2/code/okta", null),
    JHIPSTER("jhipster", "oidc", ".okta.env", "http://localhost:8080/login/oauth2/code/oidc", "groups"),
    GENERIC_WEB("other", null, ".okta.env", "http://localhost:8080/authorization-code/callback", null);

    private static final Map<String, WebAppTemplate> nameToTemplateMap = Arrays.stream(values()).collect(Collectors.toMap(it -> it.friendlyName, it -> it));

    private final String friendlyName;
    private final String springPropertyKey;
    private final String defaultConfigFileName;
    private final String defaultRedirectUri;
    private final String groupsClaim;

    WebAppTemplate(String friendlyName, String springPropertyKey, String defaultConfigFileName, String defaultRedirectUri, String groupsClaim) {
        this.friendlyName = friendlyName;
        this.springPropertyKey = springPropertyKey;
        this.defaultConfigFileName = defaultConfigFileName;
        this.defaultRedirectUri = defaultRedirectUri;
        this.groupsClaim = groupsClaim;
    }

    public String getSpringPropertyKey() {
        return springPropertyKey;
    }

    public String getDefaultConfigFileName() {
        return defaultConfigFileName;
    }

    public String getDefaultRedirectUri() {
        return defaultRedirectUri;
    }

    public String getGroupsClaim() {
        return groupsClaim;
    }

    static WebAppTemplate fromName(String name) {

        WebAppTemplate result = nameToTemplateMap.get(name);
        if (result == null) {
            throw new IllegalArgumentException("template must be empty or one of: " + nameToTemplateMap.keySet());
        }
        return result;
    }

    @Override
    public String displayName() {
        return friendlyName;
    }

    @Override
    public WebAppTemplate value() {
        return this;
    }
}
