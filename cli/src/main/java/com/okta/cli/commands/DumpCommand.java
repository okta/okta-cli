package com.okta.cli.commands;

import com.okta.cli.OktaCli;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.console.ConsoleOutput;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "dump", description = "Dump environment for debugging", hidden = true)
public class DumpCommand implements Callable<Integer> {

    @CommandLine.Mixin
    OktaCli.StandardOptions standardOptions;
    
    @Override
    public Integer call() throws Exception {

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();

        out.writeLine("Dumping environment");
        System.getenv().forEach((key, value) -> {
            out.write("\t");
            out.write(key);
            out.write(" = ");
            out.writeLine(value);
        });

        out.writeLine("System Properties");
        System.getProperties().forEach((key, value) -> {
            out.write("\t");
            out.write(key);
            out.write(" = ");
            out.writeLine(value);
        });

        out.writeLine("broken: " + new DefaultSetupService("invalid").getApiBaseUrl());

        return 0;
    }
}
