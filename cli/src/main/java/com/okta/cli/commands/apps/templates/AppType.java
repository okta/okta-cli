package com.okta.cli.commands.apps.templates;

import com.okta.cli.console.PromptOption;

public enum AppType implements PromptOption<AppType> {
    WEB("Web"),
    SPA("Single Page App"),
    NATIVE("Native App (mobile)");

    private final String friendlyName;

    AppType(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    @Override
    public String displayName() {
        return friendlyName;
    }

    @Override
    public AppType value() {
        return this;
    }
}
