package com.okta.cli.commands;

import com.okta.cli.prompter.DefaultPrompter;
import com.okta.cli.prompter.Prompter;

import java.io.File;

class Environment {

    private final File baseDir = new File(System.getProperty("user.dir"));

    private final File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");

    private final Prompter prompter = new DefaultPrompter();

    File workingDirectory() {
        return baseDir;
    }

    File getOktaPropsFile() {
        return oktaPropsFile;
    }

    Prompter prompter() {
        return prompter;
    }

    boolean isDemo() {
        return Boolean.parseBoolean(System.getenv("OKTA_CLI_DEMO"));
    }

    void debugLogging(boolean debug) {
        if (debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }
    }
}
