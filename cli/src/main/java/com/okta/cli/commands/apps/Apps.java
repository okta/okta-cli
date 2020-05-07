package com.okta.cli.commands.apps;

import com.okta.cli.OktaCli;
import com.okta.sdk.client.Clients;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "apps",
         description = "Manage Okta apps",
         subcommands = {
                    AppsConfig.class,
                    AppsCreate.class})
public class Apps implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @Override
    public Integer call() throws Exception {

        Clients.builder().build()
                .listApplications().stream()
                .forEach(app -> {
                    standardOptions.getEnvironment().getConsoleOutput().writeLine(app.getLabel() + "\t\t" + app.getId());
                });
        return 0;
    }
}
