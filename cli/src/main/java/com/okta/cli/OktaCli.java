package com.okta.cli;

import com.okta.cli.commands.DumpCommand;
import com.okta.cli.commands.JHipster;
import com.okta.cli.commands.Login;
import com.okta.cli.commands.Register;
import com.okta.cli.commands.SpringBoot;

import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.util.ArrayList;
import java.util.List;

@Command(name = "okta",
        mixinStandardHelpOptions = true, // adds --help, --version
        version = "Okta 0.1.0", // TODO this should be resolved from a resource
        description = "The Okta CLI helps you configure your applications to use Okta.",
        usageHelpAutoWidth = true,
        usageHelpWidth = 200,
        subcommands = {
                Register.class,
                Login.class,
                SpringBoot.class,
                JHipster.class,
                DumpCommand.class,
                CommandLine.HelpCommand.class,
                AutoComplete.GenerateCompletion.class})
public class OktaCli implements Runnable {

    @Spec
    private CommandSpec spec;

    @Option(names = "--verbose", description = "Verbose logging")
    private boolean verbose = false;

    @Option(names = "-D", hidden = true, description = "Set Java system property key value pairs")
    List<String> systemProperties = new ArrayList<>();

    public static void main(String... args) {
        OktaCli oktaCli = new OktaCli();
        int exitCode = new CommandLine(oktaCli).setExecutionExceptionHandler(oktaCli.new ExceptionHandler()).execute(args);
        System.exit(exitCode);
    }

    public OktaCli() {}

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Specify a command");
    }

    class ExceptionHandler implements CommandLine.IExecutionExceptionHandler {

        @Override
        public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) throws Exception {

            if (verbose) {
                ex.printStackTrace();
            } else {
                System.err.println("\nAn error occurred if you need more detail use the '--verbose' option\n");
                System.err.println(ex.getMessage());
            }

            return 1;
        }
    }
}
