package com.okta.cli.console;

import com.okta.commons.lang.Strings;

import java.util.List;
import java.util.Map;

public interface Prompter {

    String prompt(String message);

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

    default String promptUntilIfEmpty(String value, String message) {

        // prompt if empty
        if (!Strings.isEmpty(value)) {
            return value;
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

    String prompt(String message, Map<String, String> choices, String defaultChoice);

    String prompt(String message, List<String> choices, Integer defaultChoiceIndex);
}
