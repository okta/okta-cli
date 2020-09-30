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

import com.okta.cli.commands.BaseCommand;
import com.okta.sdk.client.Clients;
import picocli.CommandLine.Command;

@Command(name = "apps",
         description = "Manage Okta apps",
         subcommands = {
                    AppsConfig.class,
                    AppsCreate.class,
                    AppsDelete.class})
public class Apps extends BaseCommand {

    @Override
    public int runCommand() throws Exception {

        Clients.builder().build()
                .listApplications().stream()
                .forEach(app -> {
                    getConsoleOutput().writeLine(app.getId() + "\t" + app.getLabel());
                });
        return 0;
    }
}
