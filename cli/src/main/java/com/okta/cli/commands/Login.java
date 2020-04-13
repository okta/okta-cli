package com.okta.cli.commands;

import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.SdkConfigurationService;
import com.okta.commons.configcheck.ConfigurationValidator;
import com.okta.commons.lang.Strings;
import com.okta.sdk.impl.config.ClientConfiguration;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "login",
        description = "Authorizes the Okta CLI tool",
        mixinStandardHelpOptions = true, // adds --help, --version
        usageHelpAutoWidth = true,
        usageHelpWidth = 200)
public class Login extends BaseCommand {

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Override
    public Integer doCall() throws Exception {

        // check if okta client config exists?
        SdkConfigurationService sdkConfigurationService = new DefaultSdkConfigurationService();
        ClientConfiguration clientConfiguration = sdkConfigurationService.loadUnvalidatedConfiguration();
        String orgUrl = clientConfiguration.getBaseUrl();

        if (Strings.isEmpty(orgUrl) || Strings.isEmpty(clientConfiguration.getApiToken())) {

            if (!Strings.isEmpty(orgUrl)) {
                System.out.println("Using Okta URL: " + orgUrl);
            } else {
                orgUrl = environment.prompter().promptUntilValue(orgUrl, "Okta Org URL");
                ConfigurationValidator.assertOrgUrl(orgUrl);
            }

            System.out.println("Enter your Okta API token, for more information see: https://bit.ly/get-okta-api-token");
            String apiToken = environment.prompter().promptUntilValue(null, "Okta API token");
            ConfigurationValidator.assertApiToken(apiToken);

            sdkConfigurationService.writeOktaYaml(orgUrl, apiToken, environment.getOktaPropsFile());
        } else {
            System.out.println("Okta Org already configured: "+ orgUrl);
        }

        // TODO create cli-token client application?

        return 0;
    }

    @Override
    CommandLine.Model.CommandSpec getCommandSpec() {
        return spec;
    }
}
