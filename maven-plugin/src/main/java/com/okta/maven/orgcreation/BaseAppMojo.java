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
package com.okta.maven.orgcreation;

import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.service.ClientConfigurationException;
import com.okta.cli.common.service.ConfigFileLocatorService;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SdkConfigurationService;
import com.okta.cli.common.service.SetupService;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

abstract class BaseAppMojo extends AbstractMojo {

    /**
     * The Name / Label of the new OIDC application that will be created.  If an application with the same name already
     * exists, that application will be used.
     */
    @Parameter(property = "oidcAppName", defaultValue = "${project.name}")
    protected String oidcAppName;

    @Parameter(defaultValue = "${settings.interactiveMode}", readonly = true)
    protected boolean interactiveMode = true;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    protected File baseDir;

    protected SdkConfigurationService sdkConfigurationService = new DefaultSdkConfigurationService();

    /**
     * The id of the authorization server.
     */
    @Parameter(property = "authorizationServerId", defaultValue = "default")
    protected String authorizationServerId = "default";

    protected PrintStream out = System.out;

    void createWebApplication(String springPropertyKey, String groupClaimName, String redirectUri) throws MojoExecutionException {
        try {
            MutablePropertySource propertySource = getPropertySource();
            String baseUrl = sdkConfigurationService.loadUnvalidatedConfiguration().getBaseUrl();

            SetupService setupService = createSetupService(springPropertyKey);
            setupService.createOidcApplication(propertySource, oidcAppName, baseUrl, groupClaimName, null, authorizationServerId, interactiveMode, OpenIdConnectApplicationType.WEB, redirectUri);

            out.println("Okta application configuration has been written to: " + propertySource.getName());

        } catch (IOException | ClientConfigurationException e) {
            throw new MojoExecutionException("Failed to setup environment", e);
        }
    }

    MutablePropertySource getPropertySource() {
        return new ConfigFileLocatorService().findApplicationConfig(baseDir, new File(baseDir, ".okta.env"));
    }

    SetupService createSetupService(String springPropertyKey) {
        return new DefaultSetupService(springPropertyKey);
    }
}