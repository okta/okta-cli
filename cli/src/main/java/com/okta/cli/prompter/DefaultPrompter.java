package com.okta.cli.prompter;

import com.okta.commons.lang.Strings;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class DefaultPrompter implements Prompter, Closeable {

    private final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private final PrintWriter consoleWriter = new PrintWriter(System.out);

    @Override
    public String prompt(String message) {
        consoleWriter.write(message + ": ");
        consoleWriter.flush();
        try {
            return consoleReader.readLine();
        } catch (IOException e) {
            throw new PrompterException("Failed to read console input", e);
        }
    }

    @Override
    public String promptUntilValue(String currentValue, String promptText) {
        String value = currentValue;
        if (Strings.isEmpty(value)) {
            value = prompt(promptText);
            value = promptUntilValue(value, promptText);
        }
        return value;
    }

    @Override
    public void close() throws IOException {
        consoleWriter.close();
        consoleReader.close();
    }
}
