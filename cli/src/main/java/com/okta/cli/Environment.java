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
