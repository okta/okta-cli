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

import com.okta.cli.common.service.ClientConfigurationException;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.SdkConfigurationService;
import com.okta.commons.configcheck.ConfigurationValidator;
import com.okta.commons.lang.Strings;
import com.okta.sdk.impl.config.ClientConfiguration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static com.okta.maven.orgcreation.support.PromptUtil.promptIfNull;

@Mojo(name = "login", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject = false)
public class LoginMojo extends AbstractMojo {

    /**
     * The base Okta Organization URL. Okta Developer Orgs are commonly in the format of: {@code https://dev-12345.okta.com/}.
     */
    @Parameter(property = "orgUrl")
    protected String orgUrl;

    /**
     * Okta API token.
     */
    @Parameter(property = "apiToken")
    protected String apiToken;

    @Parameter(defaultValue = "${user.home}/.okta/okta.yaml", readonly = true)
    protected File oktaPropsFile;

    @Parameter(defaultValue = "${settings.interactiveMode}", readonly = true)
    protected boolean interactiveMode;

    @Component
    protected Prompter prompter;

    private SdkConfigurationService sdkConfigurationService = new DefaultSdkConfigurationService();

    private PrintStream out = System.out;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            // check if okta client config exists?
            ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();

            // if empty use the config values
            if (StringUtils.isEmpty(orgUrl)) {
                orgUrl = clientConfiguration.getBaseUrl();
            }

            // now prompt if needed
            if (!Strings.isEmpty(orgUrl)) {
                out.println("Using Okta URL: " + orgUrl);
            } else {
                orgUrl = promptIfNull(prompter, interactiveMode, orgUrl, "orgUrl", "Okta Org URL");
                ConfigurationValidator.assertOrgUrl(orgUrl);
            }

            if (StringUtils.isEmpty(apiToken)) {
                apiToken = clientConfiguration.getApiToken();
            }

            // now prompt if needed
            if (Strings.isEmpty(apiToken)) {
                out.println("Enter your Okta API token, for more information see: https://bit.ly/get-okta-api-token");
                apiToken = promptIfNull(prompter, interactiveMode, apiToken, "apiToken", "Okta API token");
                ConfigurationValidator.assertApiToken(apiToken);
            }

            // write the config
            sdkConfigurationService.writeOktaYaml(orgUrl, apiToken, oktaPropsFile);

        } catch (ClientConfigurationException | IOException e) {
            throw new MojoExecutionException("Failed to login: " + e.getMessage(), e);
        }
    }
}
