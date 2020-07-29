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
import com.okta.cli.common.model.RegistrationQuestions;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import com.okta.cli.console.PromptOption;
import com.okta.cli.console.Prompter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;
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

    protected CliRegistrationQuestions registrationQuestions() {
        return new CliRegistrationQuestions();
    }

    @Override
    public Integer call() throws Exception {

        CliRegistrationQuestions registrationQuestions = registrationQuestions();

        SetupService setupService = new DefaultSetupService(null);
        OrganizationResponse orgResponse = setupService.createOktaOrg(registrationQuestions,
                                   standardOptions.getEnvironment().getOktaPropsFile(),
                                   standardOptions.getEnvironment().isDemo(),
                                   standardOptions.getEnvironment().isInteractive());

        String identifier = orgResponse.getId();
        setupService.verifyOktaOrg(identifier,
                registrationQuestions,
                standardOptions.getEnvironment().getOktaPropsFile());

        return 0;


// TODO include demo logic?
//            if (demo) { // always prompt for user info in "demo mode", this info will not be used but it makes for a more realistic demo
//                organizationRequestSupplier.get();
//            }
    }

    private class CliRegistrationQuestions implements RegistrationQuestions {

        private final Prompter prompter = standardOptions.getEnvironment().prompter();

        @Override
        public boolean isOverwriteConfig() {
            PromptOption<Boolean> yes = PromptOption.of("Yes", Boolean.TRUE);
            PromptOption<Boolean> no = PromptOption.of("No", Boolean.FALSE);
            return prompter.promptIfEmpty(null, "Overwrite configuration file?", List.of(yes, no), yes);
        }

        @Override
        public OrganizationRequest getOrganizationRequest() {
            return new OrganizationRequest()
                    .setFirstName(prompter.promptUntilValue(firstName, "First name"))
                    .setLastName(prompter.promptUntilValue(lastName, "Last name"))
                    .setEmail(prompter.promptUntilValue(email, "Email address"))
                    .setOrganization(prompter.promptUntilValue(company, "Company"));
        }

        @Override
        public String getVerificationCode() {
            return prompter.promptUntilValue("Verification code");
        }
    }
}
