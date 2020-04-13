package com.okta.cli.prompter;

public interface Prompter {

    String prompt(String message);

    String promptUntilValue(String currentValue, String messsage);
}
