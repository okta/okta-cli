package com.okta.cli.commands;

import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.prompter.Prompter;
import com.okta.commons.lang.Strings;
import picocli.CommandLine;

import java.util.concurrent.Callable;

abstract class BaseRegistrationCommand extends BaseEnvCommand {

    @CommandLine.Option(names = "--batch", description = "Batch mode, will not prompt for user input")
    protected boolean interactive = true;

    @CommandLine.Option(names = "--email", description = "Email used when registering a new Okta account")
    protected String email;

    @CommandLine.Option(names = "--first-name", description = "First name used when registering a new Okta account")
    protected String firstName;

    @CommandLine.Option(names = "--last-name", description = "Last name used when registering a new Okta account")
    protected String lastName;

    @CommandLine.Option(names = "--company", description = "Company / organization used when registering a new Okta account")
    protected String company;

    protected OrganizationRequest organizationRequest() {
        Prompter prompter = environment.prompter();
        return new OrganizationRequest()
                .setFirstName(prompter.promptUntilValue(firstName, "First name"))
                .setLastName(prompter.promptUntilValue(lastName, "Last name"))
                .setEmail(prompter.promptUntilValue(email, "Email address"))
                .setOrganization(prompter.promptUntilValue(company, "Company"));
    }
}
