package com.okta.cli.commands.apps.templates;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum SpaAppTemplate {
    GENERIC("Single Page App", ".okta.env", "http://localhost:8080/callback");

    private static final List<String> names = Arrays.stream(values()).map(it -> it.friendlyName).collect(Collectors.toList());

    private final String friendlyName;
    private final String defaultConfigFileName;
    private final String defaultRedirectUri;

    SpaAppTemplate(String friendlyName, String defaultConfigFileName, String defaultRedirectUri) {
        this.friendlyName = friendlyName;
        this.defaultConfigFileName = defaultConfigFileName;
        this.defaultRedirectUri = defaultRedirectUri;
    }

    static SpaAppTemplate fromName(String name) {
        return Arrays.stream(values())
                .filter(it -> it.friendlyName.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("template must be empty or one of: " + names));
    }
}
