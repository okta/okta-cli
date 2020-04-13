package com.okta.cli.commands;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ConfigFileLocatorService;
import com.okta.cli.common.service.DefaultAuthorizationServerConfigureService;
import com.okta.cli.common.service.DefaultOidcAppCreator;
import com.okta.cli.common.service.DefaultOktaOrganizationCreator;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "jhipster",
         description = "Registers a new Okta OAuth 2.0 application and configures a '.okta.env' file",
         mixinStandardHelpOptions = true, // adds --help, --version
         usageHelpAutoWidth = true,
         usageHelpWidth = 200)
public class JHipster extends BaseRegistrationCommand {

    @Override
    public Integer doCall() throws Exception {

        File baseDir = new File(System.getProperty("user.dir"));
        File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");
        String springPropertyKey = "oidc";

        // TODO these should be options?
        String groupClaim = "groups";

        MutablePropertySource propertySource = new ConfigFileLocatorService().findApplicationConfig(baseDir, new File(baseDir, ".okta.env"));

        SetupService setupService = new DefaultSetupService(new DefaultSdkConfigurationService(), new DefaultOktaOrganizationCreator(), new DefaultOidcAppCreator(), new DefaultAuthorizationServerConfigureService(), springPropertyKey);

        setupService.configureEnvironment(this::organizationRequest, oktaPropsFile, propertySource, oidcAppName, groupClaim, authorizationServerId, environment.isDemo(), interactive, springBasedRedirectUris(springPropertyKey));

        return 0;
    }
}
