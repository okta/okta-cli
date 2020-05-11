package com.okta.cli.commands.apps.templates;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum NativeAppTemplate {

    GENERIC("Native App", ".okta.env", "com.oktapreview.dev-259824:/callback", "com.oktapreview.dev-259824:/");

    private static final List<String> names = Arrays.stream(values()).map(it -> it.friendlyName).collect(Collectors.toList());

    private final String friendlyName;
    private final String defaultConfigFileName;
    private final String defaultRedirectUri;
    private final String defaultLogoutUri;

    NativeAppTemplate(String friendlyName, String defaultConfigFileName, String defaultRedirectUri, String defaultLogoutUri) {
        this.friendlyName = friendlyName;
        this.defaultConfigFileName = defaultConfigFileName;
        this.defaultRedirectUri = defaultRedirectUri;
        this.defaultLogoutUri = defaultLogoutUri;
    }

    static NativeAppTemplate fromName(String name) {
        return Arrays.stream(values())
                .filter(it -> it.friendlyName.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("template must be empty or one of: " + names));
    }
}
