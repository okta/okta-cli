package com.okta.cli;

import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.DefaultPrompter;
import com.okta.cli.console.Prompter;

import java.io.File;

public class Environment {

    private final File baseDir = new File(System.getProperty("user.dir"));

    private final File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");

    private final ConsoleOutput consoleOutput = ConsoleOutput.create(true); // TODO force into constructor

    private final Prompter prompter = new DefaultPrompter(consoleOutput);

    public File workingDirectory() {
        return baseDir;
    }

    public File getOktaPropsFile() {
        return oktaPropsFile;
    }

    public Prompter prompter() {
        return prompter;
    }

    public boolean isDemo() {
        return Boolean.parseBoolean(System.getenv("OKTA_CLI_DEMO"));
    }

    public ConsoleOutput getConsoleOutput() {
        return consoleOutput;
    }
}
