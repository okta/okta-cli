package com.okta.cli.commands;

import com.okta.cli.commands.apps.AppCreationMixin;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import com.okta.commons.lang.Strings;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;

@Command(name = "jhipster",
         description = "Registers a new Okta OAuth 2.0 application and configures a '.okta.env' file",
         hidden = true)
public class JHipster extends BaseRegistrationCommand {

    @CommandLine.Mixin
    private AppCreationMixin appCreationMixin;

    @Override
    public Integer call() throws Exception {

        File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");
        String springPropertyKey = "oidc";

        // TODO these should be options?
        String groupClaim = "groups";

        SetupService setupService = new DefaultSetupService(springPropertyKey);

        setupService.configureEnvironment(this::organizationRequest,
                                          oktaPropsFile,
                                          appCreationMixin.getPropertySource(".okta.env"),
                                          appCreationMixin.getOrDefaultAppName(),
                                          groupClaim,
                                          Strings.isEmpty(appCreationMixin.authorizationServerId) ? "default" : appCreationMixin.authorizationServerId,
                                          standardOptions.getEnvironment().isDemo(),
                                          interactive,
                                          appCreationMixin.springBasedRedirectUris(springPropertyKey));
        return 0;
    }
}
