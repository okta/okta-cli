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
package com.okta.cli.commands.profiles;

import com.okta.cli.commands.BaseCommand;
import com.okta.cli.common.model.OktaProfile;
import com.okta.cli.common.service.DefaultProfileConfigurationService;
import com.okta.cli.common.service.ProfileConfigurationService;
import com.okta.cli.console.ConsoleOutput;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;

@CommandLine.Command(name = "delete",
        description = "Delete a profile")
public class ProfilesDelete extends BaseCommand {

    @CommandLine.Parameters(index = "0", description = "The profile name to delete")
    private String profileName;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Skip confirmation prompt")
    private boolean force;

    @Override
    public int runCommand() throws Exception {
        ProfileConfigurationService profileService = new DefaultProfileConfigurationService();
        File configFile = getEnvironment().getOktaPropsFile();

        try (ConsoleOutput out = getConsoleOutput()) {
            // Check if profile exists
            Optional<OktaProfile> profile = profileService.getProfile(configFile, profileName);
            if (profile.isEmpty()) {
                out.writeError("Profile '" + profileName + "' not found.");
                return 1;
            }

            // Check if it's the active profile
            String activeProfile = profileService.getActiveProfileName(configFile);
            if (profileName.equals(activeProfile)) {
                out.writeError("Cannot delete the active profile '" + profileName + "'.");
                out.writeLine("Switch to another profile first using 'okta profiles use <name>'.");
                return 1;
            }

            // Confirm deletion
            if (!force) {
                out.writeLine("Profile: " + profileName);
                out.writeLine("Org URL: " + profile.get().getOrgUrl());
                if (!getPrompter().promptYesNo("Delete this profile?", false)) {
                    out.writeLine("Cancelled.");
                    return 0;
                }
            }

            // Delete the profile
            boolean deleted = profileService.deleteProfile(configFile, profileName);
            if (deleted) {
                out.writeLine("Profile '" + profileName + "' deleted.");
            } else {
                out.writeError("Failed to delete profile '" + profileName + "'.");
                return 1;
            }
        }

        return 0;
    }
}
