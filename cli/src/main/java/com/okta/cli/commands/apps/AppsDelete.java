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
import com.okta.cli.console.ConsoleOutput;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.ResourceException;
import com.okta.sdk.resource.application.Application;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "delete",
        description = "Deletes an Okta app")
public class AppsDelete extends BaseCommand {

    @CommandLine.Parameters(index="0..*", arity = "1..*", description = "List of application IDs to be deleted")
    private List<String> appIds;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Deactivate and delete applications.")
    private boolean force = false;

    @Override
    public int runCommand() throws Exception {

        int exitCode = 0;
        ConsoleOutput out = getConsoleOutput();

        Client client = Clients.builder().build();

        for(String id : appIds) {
            try {
                // try to delete each one, ignoring errors, this is similar to how `docker rmi` works
                if (!deleteApp(client.getApplication(id))) {
                    exitCode = 1;
                }
            } catch (ResourceException e) {
                out.writeLine("Failed to delete application: '" + id + "':");
                out.writeLine("  " + e.getMessage());
                if (getEnvironment().isVerbose()) {
                    e.printStackTrace();
                }
                exitCode = 1;
            }
        }

        return exitCode;
    }

    private boolean deleteApp(Application app) {
        ConsoleOutput out = getConsoleOutput();

        // application already deleted
        if (Application.StatusEnum.DELETED.equals(app.getStatus())) {
            out.writeLine("Application '" + app.getId() + "' has already been marked for deletion");
            return false;
        }

        // Not interactive or --force
        if (!force && !getEnvironment().isInteractive()) {
            out.writeLine("Application '" + app.getId() + "' has not been deactivated, use '--force' to delete it");
            return false;
        }

        // prompt if needed
        if (force || getPrompter()
                 .promptYesNo("Deactivate and delete application '" + app.getId() + "'?", false)) {

            if (Application.StatusEnum.ACTIVE.equals(app.getStatus())) {
                app.deactivate();
            }
            app.delete();
            out.writeLine("Application '" + app.getId() + "' has been deleted");
            return true;
        }
        return false;
    }
}
