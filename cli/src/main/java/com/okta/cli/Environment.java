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
import com.okta.cli.console.DisabledPrompter;
import com.okta.cli.console.Prompter;

import java.io.File;

public class Environment {

    private Prompter prompter;

    private ConsoleOutput consoleOutput;

    private boolean interactive = true;

    private boolean consoleColors = true;

    public boolean isInteractive() {
        return interactive;
    }

    public Environment setInteractive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }

    public Environment setConsoleColors(boolean consoleColors) {
        this.consoleColors = consoleColors;
        return this;
    }

    public File workingDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    public File getOktaPropsFile() {
        return new File(System.getProperty("user.home"), ".okta/okta.yaml");
    }

    public boolean isDemo() {
        return Boolean.parseBoolean(System.getenv("OKTA_CLI_DEMO"));
    }

    public Prompter prompter() {
        if (prompter == null) {
            prompter = interactive
                    ? new DefaultPrompter(getConsoleOutput())
                    : new DisabledPrompter();
        }
        return prompter;
    }

    public ConsoleOutput getConsoleOutput() {
        if (consoleOutput == null) {
            consoleOutput = ConsoleOutput.create(consoleColors);
        }

        return consoleOutput;
    }
}
