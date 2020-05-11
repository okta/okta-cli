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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DefaultPrompter implements Prompter, Closeable {

    private final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    private final ConsoleOutput out;

    public DefaultPrompter() {
        this(ConsoleOutput.create(false));
    }

    public DefaultPrompter(ConsoleOutput consoleOutput) {
        this.out = consoleOutput;
    }

    @Override
    public String prompt(String message) {
        out.write(message + ": ");
        out.flush();
        try {
            return consoleReader.readLine();
        } catch (IOException e) {
            throw new PrompterException("Failed to read console input", e);
        }
    }

    @Override
    public String prompt(String message, List<String> choices, Integer defaultChoiceIndex) {

        // adjust for 1-based indexing

        String defaultChoice = defaultChoiceIndex != null ? Integer.toString(defaultChoiceIndex + 1) : null;
        Map<String, String> choicesMap = IntStream.range(0, choices.size())
                .boxed()
                .collect(Collectors.toMap(index -> Integer.toString(index + 1), choices::get));

        String key = prompt(message, choicesMap, defaultChoice);
        int keyIndex = Integer.parseInt(key) - 1;

        return choices.get(keyIndex);
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
    public String prompt(String message, Map<String, String> choices, String defaultChoice) {
        out.write(message + "\n");
        choices.forEach((key, value) -> {
            out.bold("> " + key + ": ");
            out.write(value + "\n");
        });

        String prompt = "Enter your choice";
        if (defaultChoice != null) {
            prompt += " [" + defaultChoice + "]";
        }

        String result;
        if (defaultChoice != null) {
            result = prompt(prompt);
            result = Strings.isEmpty(result) ? defaultChoice : result;
        } else {
            result = promptUntilValue(prompt);
        }

        if (!choices.containsKey(result)) {
            out.writeError("\nInvalid choice, try again\n\n");
            return prompt(message, choices, defaultChoice);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        out.close();
        consoleReader.close();
    }


    private static <T, K, U> Collector<T, ?, Map<K,U>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper,
                                                                       Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper,
                (u, v) -> {
                    throw new IllegalStateException(String.format( "Duplicate key (attempted merging values %s and %s)", u, v));
                },
                LinkedHashMap::new);
    }
}
