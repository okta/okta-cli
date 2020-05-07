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
