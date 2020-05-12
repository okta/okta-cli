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

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.OrganizationRequest;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

abstract class BaseSetupMojo extends AbstractMojo {

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
     * The Name / Label of the new OIDC application that will be created.  If an application with the same name already
     * exists, that application will be used.
     */
    @Parameter(property = "oidcAppName", defaultValue = "${project.name}")
    protected String oidcAppName;

    /**
     * The id of the authorization server.
     */
    @Parameter(property = "authorizationServerId", defaultValue = "default")
    protected String authorizationServerId = "default";


    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    protected File baseDir;

    @Parameter(defaultValue = "${user.home}/.okta/okta.yaml", readonly = true)
    protected File oktaPropsFile;

    /**
     * Set {@code demo} to {@code true} to force the prompt to collect user info.
     * This makes for a better demo without needing to create a new Okta Organization each time.
     * <p><b>NOTE:</b> Most users will ignore this property.
     */
    @Parameter(property = "okta.demo", defaultValue = "false")
    protected boolean demo = false;

    @Component
    protected Prompter prompter;


    protected OrganizationRequest organizationRequest() {
        return new OrganizationRequest()
                .setFirstName(promptIfNull(firstName, "firstName", "First name"))
                .setLastName(promptIfNull(lastName, "lastName", "Last name"))
                .setEmail(promptIfNull(email, "email", "Email address"))
                .setOrganization(promptIfNull(company, "company", "Company"));
    }

    private String promptIfNull(String currentValue, String keyName, String promptText) {

        String value = currentValue;

        if (StringUtils.isEmpty(value)) {
            if (settings.isInteractiveMode()) {
                try {
                    value = prompter.prompt(promptText);
                    value = promptIfNull(value, keyName, promptText);
                }
                catch (PrompterException e) {
                    throw new RuntimeException( e.getMessage(), e );
                }
            } else {
                throw new IllegalArgumentException( "You must specify the '" + keyName + "' property either on the command line " +
                                                    "-D" + keyName + "=... or run in interactive mode" );
            }
        }
        return value;
    }

    abstract MutablePropertySource getPropertySource();

    SetupService createSetupService(String springPropertyKey) {
        return new DefaultSetupService(springPropertyKey);
    }

    protected String[] springBasedRedirectUris(String key) {
        return new String[] {"http://localhost:8080/callback", // Okta Dev console defaults
                "http://localhost:8080/login/oauth2/code/" + key};
    }
}
