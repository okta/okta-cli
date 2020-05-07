package com.okta.cli.commands;

import com.okta.cli.OktaCli;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.SdkConfigurationService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.commons.configcheck.ConfigurationValidator;
import com.okta.commons.lang.Strings;
import com.okta.sdk.impl.config.ClientConfiguration;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "login",
        description = "Authorizes the Okta CLI tool")
public class Login implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @Override
    public Integer call() throws Exception {

        // check if okta client config exists?
        SdkConfigurationService sdkConfigurationService = new DefaultSdkConfigurationService();
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();
        String orgUrl = clientConfiguration.getBaseUrl();

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();

        if (Strings.isEmpty(orgUrl) || Strings.isEmpty(clientConfiguration.getApiToken())) {

            if (!Strings.isEmpty(orgUrl)) {
                out.writeLine("Using Okta URL: " + orgUrl);
            } else {
                orgUrl = standardOptions.getEnvironment().prompter().promptUntilValue(orgUrl, "Okta Org URL");
                ConfigurationValidator.assertOrgUrl(orgUrl);
            }

            System.out.println("Enter your Okta API token, for more information see: https://bit.ly/get-okta-api-token");
            String apiToken = standardOptions.getEnvironment().prompter().promptUntilValue(null, "Okta API token");
            ConfigurationValidator.assertApiToken(apiToken);

            sdkConfigurationService.writeOktaYaml(orgUrl, apiToken, standardOptions.getEnvironment().getOktaPropsFile());
        } else {
            out.writeLine("Okta Org already configured: "+ orgUrl);
        }

        // TODO create cli-token client application?

        return 0;
    }
}
