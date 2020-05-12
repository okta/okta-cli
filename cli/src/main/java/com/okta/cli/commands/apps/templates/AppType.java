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
package com.okta.cli.commands.apps.templates;

import com.okta.cli.console.PromptOption;

public enum AppType implements PromptOption<AppType> {
    WEB("Web"),
    SPA("Single Page App"),
    NATIVE("Native App (mobile)"),
    SERVICE("Service (Machine-to-Machine)");

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
