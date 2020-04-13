package com.okta.cli.commands;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ConfigFileLocatorService;
import com.okta.cli.common.service.DefaultAuthorizationServerConfigureService;
import com.okta.cli.common.service.DefaultOidcAppCreator;
import com.okta.cli.common.service.DefaultOktaOrganizationCreator;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;

@Command(name = "spring-boot",
         description = "Registers a new Okta OAuth 2.0 application and configures Spring's application properties",
         mixinStandardHelpOptions = true, // adds --help, --version
         usageHelpAutoWidth = true,
         usageHelpWidth = 200)
public class SpringBoot extends BaseRegistrationCommand {

    @CommandLine.Option(names = "--use-standard-spring-properties", description = "Defaults to 'true', and uses 'okta.oauth2.*' property format")
    protected boolean useStandardSpringProperties = false;

    @Override
    public Integer doCall() throws Exception {

        File baseDir = new File(System.getProperty("user.dir"));
        File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");
        String springPropertyKey = useStandardSpringProperties ? "okta" : null;

        // TODO these should be options
        String groupClaim = null;

        MutablePropertySource propertySource = new ConfigFileLocatorService().findApplicationConfig(baseDir, applicationConfigFile);

        SetupService setupService = new DefaultSetupService(new DefaultSdkConfigurationService(), new DefaultOktaOrganizationCreator(), new DefaultOidcAppCreator(), new DefaultAuthorizationServerConfigureService(), springPropertyKey);

        setupService.configureEnvironment(this::organizationRequest, oktaPropsFile, propertySource, oidcAppName, groupClaim, authorizationServerId, environment.isDemo(), interactive, springBasedRedirectUris("okta"));

        return 0;
    }
}
