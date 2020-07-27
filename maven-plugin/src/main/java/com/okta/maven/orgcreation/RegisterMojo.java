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
package com.okta.maven.orgcreation;

import com.okta.cli.common.model.OrganizationResponse;
import com.okta.maven.orgcreation.service.DefaultMavenRegistrationService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.Prompter;

import java.io.File;

/**
 * Signs up for a free Okta Developer Account.  Prompts for basic user info, accounts will be activated via email.
 */
@Mojo(name = "register", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class RegisterMojo extends AbstractMojo {

    /**
     * Email used when registering a new Okta account.
     */
    @Parameter(property = "email")
    protected String email;

    /**
     * First name used when registering a new Okta account.
     */
    @Parameter(property = "firstName")
    protected String firstName;

    /**
     * Last name used when registering a new Okta account.
     */
    @Parameter(property = "lastName")
    protected String lastName;

    /**
     * Company / organization used when registering a new Okta account.
     */
    @Parameter(property = "company")
    protected String company;

    /**
     * Set {@code demo} to {@code true} to force the prompt to collect user info.
     * This makes for a better demo without needing to create a new Okta Organization each time.
     * <p><b>NOTE:</b> Most users will ignore this property.
     */
    @Parameter(property = "okta.demo", defaultValue = "false")
    protected boolean demo = false;

    @Parameter(defaultValue = "${settings.interactiveMode}", readonly = true)
    protected boolean interactiveMode;

    @Parameter(defaultValue = "${user.home}/.okta/okta.yaml", readonly = true)
    protected File oktaPropsFile;

    @Component
    protected Prompter prompter;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        DefaultMavenRegistrationService registrationService = new DefaultMavenRegistrationService(prompter, oktaPropsFile, demo, interactiveMode);
        OrganizationResponse response = registrationService.register(firstName, lastName, email, company);
        registrationService.verify(response.getIdentifier(), null);
    }
}
