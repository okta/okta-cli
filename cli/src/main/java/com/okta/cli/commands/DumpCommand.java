package com.okta.cli.commands;

import com.okta.cli.common.service.DefaultSetupService;
import picocli.CommandLine;

@CommandLine.Command(name = "dump", description = "Dump environment for debugging", hidden = true)
public class DumpCommand extends BaseCommand {

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Override
    Integer doCall() throws Exception {

        System.out.println("Dumping environment");
        System.getenv().forEach((key, value) -> {
            System.out.print("\t");
            System.out.print(key);
            System.out.print(" = ");
            System.out.println(value);
        });

        System.out.println("System Properties");
        System.getProperties().forEach((key, value) -> {
            System.out.print("\t");
            System.out.print(key);
            System.out.print(" = ");
            System.out.println(value);
        });

        System.out.println("broken: " + new DefaultSetupService("invalid").getApiBaseUrl());

        return 0;
    }

    @Override
    CommandLine.Model.CommandSpec getCommandSpec() {
        return spec;
    }
}
