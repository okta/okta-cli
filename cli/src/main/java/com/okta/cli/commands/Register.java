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
import com.okta.cli.common.model.OidcProperties;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.cli.common.model.RegistrationQuestions;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.Prompter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "register",
         description = "Sign up for a new Okta account")
public class Register extends BaseCommand {

    @CommandLine.Option(names = "--email", description = "Email used when registering a new Okta account.")
    protected String email;

    @CommandLine.Option(names = "--first-name", description = "First name used when registering a new Okta account.")
    protected String firstName;

    @CommandLine.Option(names = "--last-name", description = "Last name used when registering a new Okta account.")
    protected String lastName;

    @CommandLine.Option(names = "--company", description = "Company/organization used when registering a new Okta account.")
    protected String company;

    @CommandLine.Option(names = "--country", description = "Country of residence")
    protected String country;

    public Register() {}

    private Register(OktaCli.StandardOptions standardOptions) {
        super(standardOptions);
    }

    // TODO, these registration bit needs to be refactor out into a service, so that this stanardOptions object does NOT need to be passed around
    public static void requireRegistration(OktaCli.StandardOptions standardOptions) throws Exception {
        if (!new DefaultSdkConfigurationService().isConfigured()) {
            ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();
            out.writeLine("Registering for a new Okta account, if you would like to use an existing account, use 'okta login' instead.\n");
            new Register(standardOptions).call();
            out.writeLine("");
            standardOptions.getEnvironment().prompter().pause();
        }
    }

    protected CliRegistrationQuestions registrationQuestions() {
        return new CliRegistrationQuestions(this);
    }

    @Override
    public int runCommand() throws Exception {

        CliRegistrationQuestions registrationQuestions = registrationQuestions();

        SetupService setupService = new DefaultSetupService(OidcProperties.oktaEnv());
        OrganizationResponse orgResponse = setupService.createOktaOrg(registrationQuestions,
                                   getEnvironment().getOktaPropsFile(),
                                   getEnvironment().isDemo(),
                                   getEnvironment().isInteractive());

        String identifier = orgResponse.getDeveloperOrgCliToken();
        setupService.verifyOktaOrg(identifier,
                registrationQuestions,
                getEnvironment().getOktaPropsFile());

        return 0;


// TODO include demo logic?
//            if (demo) { // always prompt for user info in "demo mode", this info will not be used but it makes for a more realistic demo
//                organizationRequestSupplier.get();
//            }
    }

    private class CliRegistrationQuestions extends ConfigQuestions implements RegistrationQuestions {

        private final Prompter prompter = getPrompter();

        CliRegistrationQuestions(BaseCommand command) {
            super(command);
        }

        @Override
        public OrganizationRequest getOrganizationRequest() {
            return new OrganizationRequest()
                    .setFirstName(prompter.promptUntilValue(firstName, "First name"))
                    .setLastName(prompter.promptUntilValue(lastName, "Last name"))
                    .setEmail(prompter.promptUntilValue(email, "Email address"))
                    .setCountry(prompter.promptUntilValue(country, "Country"));
        }
    }
}
