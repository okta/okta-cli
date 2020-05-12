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
package com.okta.cli.commands.apps;

import com.okta.cli.OktaCli;
import com.okta.sdk.client.Clients;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "apps",
         description = "Manage Okta apps",
         subcommands = {
                    AppsConfig.class,
                    AppsCreate.class})
public class Apps implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @Override
    public Integer call() throws Exception {

        Clients.builder().build()
                .listApplications().stream()
                .forEach(app -> {
                    standardOptions.getEnvironment().getConsoleOutput().writeLine(app.getId() + "\t" + app.getLabel());
                });
        return 0;
    }
}
