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
package com.okta.maven.orgcreation.service;

import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.model.OrganizationResponse;
import com.okta.cli.common.model.RegistrationQuestions;
import com.okta.cli.common.service.ClientConfigurationException;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.components.interactivity.Prompter;

import java.io.File;
import java.io.IOException;

import static com.okta.maven.orgcreation.support.PromptUtil.promptIfNull;
import static com.okta.maven.orgcreation.support.PromptUtil.promptYesNo;

public class DefaultMavenRegistrationService implements MavenRegistrationService {

    private final Prompter prompter;
    private final File oktaPropsFile;
    private final boolean demo;
    private final boolean interactive;

    public DefaultMavenRegistrationService(Prompter prompter, File oktaPropsFile, boolean demo, boolean interactive) {
        this.prompter = prompter;
        this.oktaPropsFile = oktaPropsFile;
        this.demo = demo;
        this.interactive = interactive;
    }

    @Override
    public OrganizationResponse register(String firstName, String lastName, String email, String company) throws MojoExecutionException {
        try {
            SetupService setupService = new DefaultSetupService(null);
            RegistrationQuestions registrationQuestions = new MavenPromptingRegistrationQuestions()
                    .setFirstName(firstName)
                    .setLastName(lastName)
                    .setEmail(email)
                    .setCompany(company);
            return setupService.createOktaOrg(registrationQuestions,
                                              oktaPropsFile,
                                              demo,
                                              interactive);
        } catch (IOException | ClientConfigurationException e) {
            throw new MojoExecutionException("Failed to register account: " + e.getMessage(), e);
        }
    }

    @Override
    public void verify(String identifier, String code) throws MojoExecutionException {
        try {
            SetupService setupService = new DefaultSetupService(null);
            RegistrationQuestions registrationQuestions = new MavenPromptingRegistrationQuestions()
                    .setCode(code);
            setupService.verifyOktaOrg(identifier, registrationQuestions, oktaPropsFile);
        } catch (IOException | ClientConfigurationException e) {
            throw new MojoExecutionException("Failed to register account: " + e.getMessage(), e);
        }
    }

    private OrganizationRequest organizationRequest(String firstName, String lastName, String email, String company) {
        return new OrganizationRequest()
                .setFirstName(promptIfNull(prompter, interactive, firstName, "firstName", "First name"))
                .setLastName(promptIfNull(prompter, interactive, lastName, "lastName", "Last name"))
                .setEmail(promptIfNull(prompter, interactive, email, "email", "Email address"))
                .setOrganization(promptIfNull(prompter, interactive, company, "company", "Company"));
    }

    private String codePrompt(String code) {
        return promptIfNull(prompter, interactive, code, "code", "Verification Code");
    }

    private boolean overwritePrompt(Boolean overwrite) {
        return promptYesNo(prompter, interactive, overwrite, "overwriteConfig", "Overwrite configuration file?");
    }

    @Data
    @Accessors(chain = true)
    private class MavenPromptingRegistrationQuestions implements RegistrationQuestions {

        private String firstName;
        private String lastName;
        private String email;
        private String company;

        private String code;

        private Boolean overwriteConfig;

        @Override
        public boolean isOverwriteConfig() {
            return overwritePrompt(overwriteConfig);
        }

        @Override
        public OrganizationRequest getOrganizationRequest() {
            return organizationRequest(firstName, lastName, email, company);
        }

        @Override
        public String getVerificationCode() {
            return codePrompt(code);
        }
    }
}
