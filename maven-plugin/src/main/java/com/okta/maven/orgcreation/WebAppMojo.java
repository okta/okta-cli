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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Creates a new Okta OIDC Web Application for use with your Web Application and writes an {@code .okta.env} file with it's configuration.
 * <p>
 * NOTE: You must have an existing Okta account to use this Mojo, use the goals: {@code okta:register} or {@code okta:login}.
 * <p>
 * To create other types of applications on the command line see the <a href="https://github.com/oktadeveloper/okta-maven-plugin">Okta CLI</a>.
 */
@Mojo(name = "web-app", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
public class WebAppMojo extends BaseAppMojo {

    /**
     * The redirect URI used for the OIDC application.
     */
    @Parameter(property = "redirectUri", defaultValue = "http://localhost:8080/callback")
    protected String redirectUri = "http://localhost:8080/callback";

    @Override
    public void execute() throws MojoExecutionException {
        createWebApplication(null, null, redirectUri);
    }
}