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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DefaultPrompter implements Prompter, Closeable {

    private final BufferedReader consoleReader;

    private final ConsoleOutput out;

    public DefaultPrompter(ConsoleOutput consoleOutput) {
        this.out = consoleOutput;
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    @Override
    public String prompt(String message) {
        out.write(message + ": ");
        return prompt();
    }

    private String prompt() {
        out.flush();
        try {
            return consoleReader.readLine();
        } catch (IOException e) {
            throw new PrompterException("Failed to read console input", e);
        }
    }

    public boolean promptYesNo(String message, boolean defaultYes) {

        out.write(message + " [");
        if (defaultYes) {
            out.bold("Y");
            out.write("/n");
        } else {
            out.write("y/");
            out.bold("N");
        }
        out.write("]");

        String resultText = prompt();
        if (Strings.isEmpty(resultText)) {
            return defaultYes;
        } else if (resultText.equalsIgnoreCase("y") || resultText.equalsIgnoreCase("yes")) {
            return true;
        } else if (resultText.equalsIgnoreCase("n") || resultText.equalsIgnoreCase("no")) {
            return false;
        } else {
            out.writeError("\nInvalid choice, try again\n\n");
            return promptYesNo(message, defaultYes);
        }
    }

    @Override
    public <T> T prompt(String message, List<PromptOption<T>> options, PromptOption<T> defaultChoice) {

        Map<String, PromptOption<T>> choices = IntStream.range(0, options.size())
                .boxed()
                .collect(Collectors.toMap(index -> Integer.toString(index + 1), options::get));

        out.write(message + "\n");
        choices.forEach((key, value) -> {
            out.bold("> " + key + ": ");
            out.write(value.displayName() + "\n");
        });

        String prompt = "Enter your choice";
        if (defaultChoice != null) {
            prompt += " [" + defaultChoice.displayName() + "]";
        }

        String resultText;
        if (defaultChoice != null) {
            resultText = prompt(prompt);

            // if empty response, use default value
            if (Strings.isEmpty(resultText)) {
                return defaultChoice.value();
            }
        } else {
            resultText = promptUntilValue(prompt);
        }

        if (!choices.containsKey(resultText)) {
            out.writeError("\nInvalid choice, try again\n\n");
            return prompt(message, options, defaultChoice);
        }

        return choices.get(resultText).value();
    }

    @Override
    public void close() throws IOException {
        out.close();
        consoleReader.close();
    }
}