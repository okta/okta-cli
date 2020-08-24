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

import com.okta.cli.commands.DumpCommand;
import com.okta.cli.commands.Login;
import com.okta.cli.commands.Register;
import com.okta.cli.commands.Start;
import com.okta.cli.commands.apps.Apps;
import com.okta.commons.lang.ApplicationInfo;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.List;

@Command(name = "okta",
        description = "The Okta CLI helps you configure your applications to use Okta.",
        subcommands = {
                Register.class,
                Login.class,
                Apps.class,
                Start.class,
                DumpCommand.class,
                CommandLine.HelpCommand.class,
                AutoComplete.GenerateCompletion.class})
public class OktaCli implements Runnable {

    @Spec
    private CommandSpec spec;

    @CommandLine.Mixin
    private StandardOptions standardOptions;

    public static void main(String... args) {
        System.exit(run(args));
    }

    public static int run(String... args) {
        OktaCli oktaCli = new OktaCli();
        CommandLine commandLine = new CommandLine(oktaCli)
                .setExecutionExceptionHandler(oktaCli.new ExceptionHandler())
                .setExecutionStrategy(new CommandLine.RunLast())
                .setUsageHelpAutoWidth(true)
                .setUsageHelpWidth(200);
        return commandLine.execute(args);
    }

    public OktaCli() {}

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Specify a command");
    }

    class ExceptionHandler implements CommandLine.IExecutionExceptionHandler {

        @Override
        public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {

            // TODO get the root cause exception

            // `null` is the typical message for an NPE, so print the stack traces
            if (standardOptions.isVerbose()|| ex instanceof NullPointerException) {
                ex.printStackTrace();
            } else {
                System.err.println("\nAn error occurred if you need more detail use the '--verbose' option\n");
                System.err.println(ex.getMessage());
            }

            return 1;
        }
    }

    /**
     * Standard options, Java System properties, verbose logging, help, version, etc
     */
    @Command(versionProvider = VersionProvider.class, mixinStandardHelpOptions = true)
    public static class StandardOptions {

        private final static Environment environment = new Environment();

        private boolean verbose = false;

        @Option(names = "--color", hidden = true) // gnu tools use --color=always,never,auto (for not just support always and never)
        public void setColor(ColorOptions color) {
            environment.setConsoleColors(color == ColorOptions.always);
        }

        @Option(names = "--batch", description = "Batch mode, will not prompt for user input")
        public void setBatch(boolean batch) {
            environment.setInteractive(!batch);
        }

        @Option(names = "--verbose", description = "Verbose logging")
        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
            if (verbose) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            }
        }

        public boolean isVerbose() {
            return verbose;
        }

        @Option(names = "-D", hidden = true, description = "Set Java System Property key value pairs", paramLabel = "<key=value>")
        public void setSystemProperties(List<String> props) {
            if (props != null) {
                props.forEach(it -> {
                    String[] keyValue = it.split("=", 2);
                    String key = keyValue[0];
                    String value = "";
                    if (keyValue.length == 2) { // TODO: fail here if not 2?
                        value = keyValue[1];
                    }
                    System.setProperty(key, value);
                });
            }
        }

        public Environment getEnvironment() {
            return environment;
        }
    }

    public enum ColorOptions {
        never, always
    }

    public static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() throws Exception {
            String version = ApplicationInfo.get().get("okta-cli");
            return new String[] {version };
        }
    }
}
