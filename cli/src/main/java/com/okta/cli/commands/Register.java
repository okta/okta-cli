package com.okta.cli.commands;

import com.okta.cli.common.service.DefaultAuthorizationServerConfigureService;
import com.okta.cli.common.service.DefaultOidcAppCreator;
import com.okta.cli.common.service.DefaultOktaOrganizationCreator;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "register",
         description = "Signs up for a new Okta account",
         mixinStandardHelpOptions = true, // adds --help, --version
         usageHelpAutoWidth = true,
         usageHelpWidth = 200)
public class Register extends BaseRegistrationCommand {

    @Override
    public Integer doCall() throws Exception {
        SetupService setupService = new DefaultSetupService(new DefaultSdkConfigurationService(),
                                                            new DefaultOktaOrganizationCreator(),
                                                            new DefaultOidcAppCreator(),
                                                            new DefaultAuthorizationServerConfigureService(),
                                                            null);

        setupService.createOktaOrg(this::organizationRequest, environment.getOktaPropsFile(), environment.isDemo(), interactive);
        return 0;
    }
}
