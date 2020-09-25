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
package com.okta.cli;

import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import picocli.CommandLine;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class BeanConfiguration {

    @Produces
    CommandLine customCommandLine(PicocliCommandLineFactory factory) {
        return factory.create()
                .setExecutionExceptionHandler(new OktaCli.ExceptionHandler())
                .setExecutionStrategy(new CommandLine.RunLast())
                .setUsageHelpAutoWidth(true)
                .setUsageHelpWidth(200);
    }
}
