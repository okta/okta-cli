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

import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.SdkConfigurationService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.commons.configcheck.ConfigurationValidator;
import com.okta.commons.lang.Strings;
import com.okta.sdk.impl.config.ClientConfiguration;
import picocli.CommandLine;

@CommandLine.Command(name = "login",
        description = "Authorizes the Okta CLI tool")
public class Login extends BaseCommand {

    @Override
    public int runCommand() throws Exception {

        // check if okta client config exists?
        SdkConfigurationService sdkConfigurationService = new DefaultSdkConfigurationService();
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();
        String orgUrl = clientConfiguration.getBaseUrl();

        ConsoleOutput out = getConsoleOutput();

        if (Strings.isEmpty(orgUrl) || Strings.isEmpty(clientConfiguration.getApiToken())) {

            if (!Strings.isEmpty(orgUrl)) {
                out.writeLine("Using Okta URL: " + orgUrl);
            } else {
                orgUrl = getPrompter().promptUntilValue(orgUrl, "Okta Org URL");
                ConfigurationValidator.assertOrgUrl(orgUrl);
            }

            out.writeLine("Enter your Okta API token, for more information see: https://bit.ly/get-okta-api-token");
            String apiToken = getPrompter().promptUntilValue(null, "Okta API token");
            ConfigurationValidator.assertApiToken(apiToken);

            sdkConfigurationService.writeOktaYaml(orgUrl, apiToken, getEnvironment().getOktaPropsFile());
        } else {
            out.writeLine("Okta Org already configured: "+ orgUrl);
        }

        return 0;
    }
}
