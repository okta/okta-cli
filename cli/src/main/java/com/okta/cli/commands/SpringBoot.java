package com.okta.cli.commands;

import com.okta.cli.commands.apps.AppCreationMixin;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import com.okta.commons.lang.Strings;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;

@Command(name = "spring-boot",
         description = "Registers a new Okta OAuth 2.0 application and configures Spring's application properties",
         hidden = true)
public class SpringBoot extends BaseRegistrationCommand {

    @CommandLine.Option(names = "--use-standard-spring-properties", description = "Defaults to 'true', and uses 'okta.oauth2.*' property format")
    private boolean useStandardSpringProperties = false;

    @CommandLine.Mixin
    private AppCreationMixin appCreationMixin;

    @Override
    public Integer call() throws Exception {

        File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");
        String springPropertyKey = useStandardSpringProperties ? "okta" : null;

        // TODO these should be options
        String groupClaim = null;

        SetupService setupService = new DefaultSetupService(springPropertyKey);

        setupService.configureEnvironment(this::organizationRequest,
                                          oktaPropsFile,
                                          appCreationMixin.getPropertySource(),
                                          appCreationMixin.getOrDefaultAppName(),
                                          groupClaim,
                                          Strings.isEmpty(appCreationMixin.authorizationServerId) ? "default" : appCreationMixin.authorizationServerId,
                                          standardOptions.getEnvironment().isDemo(),
                                          interactive,
                                          appCreationMixin.springBasedRedirectUris("okta"));
        return 0;
    }
}
