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

import com.okta.cli.OktaCli;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import com.okta.cli.console.Prompter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "register",
         description = "Sign up for a new Okta account")
public class Register implements Callable<Integer> {

    @CommandLine.Mixin
    protected OktaCli.StandardOptions standardOptions;

    @CommandLine.Option(names = "--email", description = "Email used when registering a new Okta account")
    protected String email;

    @CommandLine.Option(names = "--first-name", description = "First name used when registering a new Okta account")
    protected String firstName;

    @CommandLine.Option(names = "--last-name", description = "Last name used when registering a new Okta account")
    protected String lastName;

    @CommandLine.Option(names = "--company", description = "Company / organization used when registering a new Okta account")
    protected String company;

    protected OrganizationRequest organizationRequest() {
        Prompter prompter = standardOptions.getEnvironment().prompter();
        return new OrganizationRequest()
                .setFirstName(prompter.promptUntilValue(firstName, "First name"))
                .setLastName(prompter.promptUntilValue(lastName, "Last name"))
                .setEmail(prompter.promptUntilValue(email, "Email address"))
                .setOrganization(prompter.promptUntilValue(company, "Company"));
    }

    @Override
    public Integer call() throws Exception {
        Prompter prompter = standardOptions.getEnvironment().prompter();
        SetupService setupService = new DefaultSetupService(null);
        OrganizationResponse orgResponse = setupService.createOktaOrg(this::organizationRequest,
                                   standardOptions.getEnvironment().getOktaPropsFile(),
                                   standardOptions.getEnvironment().isDemo(),
                                   standardOptions.getEnvironment().isInteractive());

        // TODO this logic (specifically because of older, no replaced, maven code needs to be restructured
        String identifier = orgResponse.getIdentifier();
        setupService.verifyOktaOrg(identifier,
                () -> prompter.promptUntilValue("Verification code"),
                standardOptions.getEnvironment().getOktaPropsFile());

        return 0;
    }
}
