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
package com.okta.cli.console;

import java.util.List;
import java.util.Map;

public class DisabledPrompter implements Prompter {
    @Override
    public String prompt(String message) {
        throw fail(message);
    }

    @Override
    public <T> T prompt(String message, List<PromptOption<T>> promptOptions, PromptOption<T> defaultChoice) {
        throw fail(message);
    }

    @Override
    public String prompt(String message, Map<String, String> choices, String defaultChoice) {
        throw fail(message);
    }

    @Override
    public String prompt(String message, List<String> choices, Integer defaultChoiceIndex) {
        throw fail(message);
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException("Interactive mode is disable, failed to prompt for response: " + message);
    }
}
