package com.okta.cli.commands;

import picocli.CommandLine.Model.CommandSpec;

import java.util.List;
import java.util.concurrent.Callable;

abstract class BaseCommand implements Callable<Integer> {


    protected Environment environment = new Environment();

    @Override
    public Integer call() throws Exception {
        CommandSpec spec = getCommandSpec();
        environment.debugLogging(spec.parent().findOption("--verbose").getValue());

        List<String> props = spec.parent().findOption("-D").getValue();
        if (props != null) {
            props.forEach(it -> {
                String[] keyValue = it.split("=", 1);
                String key = keyValue[0];
                String value = "";
                if (keyValue.length == 2) { // TODO: fail here if not 2?
                    value = keyValue[1];
                }
                System.setProperty(key, value);
            });
        }

        return doCall();
    }

    abstract Integer doCall() throws Exception;

    abstract CommandSpec getCommandSpec();
}
