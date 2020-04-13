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
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.SetupService;
import com.okta.maven.orgcreation.support.SuppressFBWarnings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;

/**
 * Creates a free Okta Developer account and registers / configures a new OIDC application. To signup for an account
 * without using this plugin visit  <a href="https://developer.okta.com/signup">developer.okta.com</a>.
 * <p>
 * If you have an existing Okta account, you can configure this plugin to use it by configuring a
 * {@code ~/.okta/okta.yaml} configuration (or equivalent, see the <a href="https://github.com/okta/okta-sdk-java#configuration-reference">Okta SDK configuration reference</a> for more info).
 * <p>
 * TL;DR: If you have an exiting account create a <code>~/.okta/okta.yaml</code> with your URL and API token:
 * <pre><code>
 * okta:
 *   client:
 *     orgUrl: https://{yourOktaDomain}
 *     token: {yourApiToken}
 * </code></pre>
 *
 */
@Mojo(name = "jhipster", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class JHipsterMojo extends BaseSetupMojo {

    @Override
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive on Java 11")
    public void execute() throws MojoExecutionException {

        try {
            String springPropertyKey = "oidc";
            SetupService setupService = new DefaultSetupService(springPropertyKey);
            setupService.configureEnvironment(this::organizationRequest,
                    oktaPropsFile,
                    getPropertySource(),
                    oidcAppName,
                    "groups",
                    authorizationServerId,
                    demo,
                    settings.isInteractiveMode(),
                    springBasedRedirectUris(springPropertyKey));

        } catch (IOException | ClientConfigurationException e) {
            throw new MojoExecutionException("Failed to setup environment", e);
        }
    }

    @Override
    MutablePropertySource getPropertySource() {
        return new ConfigFileLocatorService().findApplicationConfig(baseDir, new File(baseDir, ".okta.env"));
    }
}