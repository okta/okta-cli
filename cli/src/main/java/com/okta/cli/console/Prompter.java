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

import com.okta.commons.lang.Strings;

import java.util.List;

public interface Prompter {

    /**
     * Displays a "(Press Enter to continue)" message and waits for user acknowledgement.
     */
    void pause();

    String prompt(String message);

    default boolean promptYesNo(String message) {
        return promptYesNo(message, true);
    }

    boolean promptYesNo(String message, boolean defaultYes);

    default String promptIfEmpty(String value, String message, String defaultValue) {

        // prompt if empty
        if (Strings.isEmpty(value)) {
            value = prompt(message + " [" + defaultValue + "]");
        }

        // fall back value
        if (Strings.isEmpty(value)) {
            value = defaultValue;
        }
        return value;
    }

    default String promptUntilIfEmpty(String value, String message, String defaultValue) {

        // prompt if empty
        if (!Strings.isEmpty(value)) {
            return value;
        }

        if (!Strings.isEmpty(defaultValue)) {
            return promptIfEmpty(value, message, defaultValue);
        }

        return promptUntilValue(message);
    }

    default String promptUntilValue(String message) {
        return promptUntilValue(null, message);
    }

    default String promptUntilValue(String currentValue, String promptText) {
        String value = currentValue;
        if (Strings.isEmpty(value)) {
            value = prompt(promptText);
            value = promptUntilValue(value, promptText);
        }
        return value;
    }

    <T> T prompt(String message, List<PromptOption<T>> options, PromptOption<T> defaultChoice);

    default <T> T promptIfEmpty(T value, String message, List<PromptOption<T>> options, PromptOption<T> defaultChoice) {
        if (value != null) {
            return value;
        }
        return prompt(message, options, defaultChoice);
    }
}
