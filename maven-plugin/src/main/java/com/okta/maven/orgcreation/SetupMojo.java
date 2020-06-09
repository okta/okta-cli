/*
 * Copyright 2018-Present Okta, Inc.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * This Mojo has been removed and split into multiple seperate goals:
 * <ul>
 *     <li>okta:register</li>
 *     <li>okta:login</li>
 *     <li>okta:spring-boot</li>
 *     <li>okta:jhipster</li>
 *     <li>okta:web-app</li>
 * </ul>
 * <b>NOTE:</b> TO use the previous version of the plugin with this goal run {@code mvn com.okta:okta-maven-plugin:0.2.0:setup}
 * @deprecated Use okta:register, okta:spring-boot, okta:web-app, and okta:jhipster instead.
 */
@Mojo(name = "setup", defaultPhase = LifecyclePhase.NONE, threadSafe = false, aggregator = true, requiresProject=false)
@Deprecated
public class SetupMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throw new MojoFailureException("This mojo has been removed and split up into `okta:register`, `okta:login`, `okta:spring-boot`, `okta:web-app`, and `okta:jhipster`");
    }
}
