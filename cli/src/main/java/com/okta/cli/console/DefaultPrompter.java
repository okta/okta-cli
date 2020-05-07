package com.okta.cli.console;

import com.okta.commons.lang.Strings;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DefaultPrompter implements Prompter, Closeable {

    private final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

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
                .mapToObj(it -> it)
                .collect(Collectors.toMap(index -> Integer.toString(index + 1), choices::get));

        String key = prompt(message, choicesMap, defaultChoice);
        int keyIndex = Integer.parseInt(key) - 1;

        return choices.get(keyIndex);
    }

    @Override
    public String prompt(String message, Map<String, String> choices, String defaultChoice) {
        out.write(message + "\n");
        choices.forEach((key, value) -> {
            bold("> " + key + ": ");
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
            writeError("\nInvalid choice, try again\n\n");
            return prompt(message, choices, defaultChoice);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        out.close();
        consoleReader.close();
    }

    private void writeError(String message) {
        writeWithColor(message, ConsoleColors.RED);
    }

    private void bold(String message) {
        writeWithColor(message, ConsoleColors.BOLD);
    }

    private void writeWithColor(String message, String ansiColor) {
        if (true) { // TODO check if ANSI
            out.write(ansiColor);
        }

        out.write(message);

        if (true) { // TODO check if ANSI
            out.write(ConsoleColors.RESET);
        }
    }
}
