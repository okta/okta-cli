package com.okta.cli.commands;

import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import picocli.CommandLine.Command;

@Command(name = "register",
         description = "Sign up for a new Okta account")
public class Register extends BaseRegistrationCommand {

    @Override
    public Integer call() throws Exception {
        SetupService setupService = new DefaultSetupService(null);
        setupService.createOktaOrg(this::organizationRequest, standardOptions.getEnvironment().getOktaPropsFile(), standardOptions.getEnvironment().isDemo(), interactive);
        return 0;
    }
}
