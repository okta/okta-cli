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
package com.okta.cli.commands;

import com.okta.cli.Environment;
import com.okta.cli.OktaCli;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.Prompter;
import picocli.CommandLine;

import java.util.concurrent.Callable;

public abstract class BaseCommand implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    public BaseCommand() {}

    public BaseCommand(OktaCli.StandardOptions standardOptions) {
        this.standardOptions = standardOptions;
    }

    protected abstract int runCommand() throws Exception;

    @Override
    public Integer call() throws Exception {
        return runCommand();
    }

    protected OktaCli.StandardOptions getStandardOptions() {
        return standardOptions;
    }

    protected ConsoleOutput getConsoleOutput() {
        return standardOptions.getEnvironment().getConsoleOutput();
    }

    protected Prompter getPrompter() {
        return standardOptions.getEnvironment().prompter();
    }

    protected Environment getEnvironment() {
        return standardOptions.getEnvironment();
    }

    protected ConfigQuestions configQuestions() {
        return new ConfigQuestions(this);
    }

    protected static class ConfigQuestions {

        private final BaseCommand command;

        ConfigQuestions(BaseCommand command) {
            this.command = command;
        }

        public boolean isOverwriteExistingConfig(String oktaBaseUrl, String configFile) {
            command.getConsoleOutput().writeLine("An existing Okta Organization (" + oktaBaseUrl + ") was found in " + configFile);
            return command.getPrompter().promptYesNo("Overwrite configuration file?");
        }
    }
}
