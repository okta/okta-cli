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

@CommandLine.Command(name = "use",
        description = "Switch to a different profile")
public class ProfilesUse extends BaseCommand {

    @CommandLine.Parameters(index = "0", description = "The profile name to switch to")
    private String profileName;

    @Override
    public int runCommand() throws Exception {
        ProfileConfigurationService profileService = new DefaultProfileConfigurationService();
        File configFile = getEnvironment().getOktaPropsFile();

        try (ConsoleOutput out = getConsoleOutput()) {
            // Check if profile exists
            Optional<OktaProfile> profile = profileService.getProfile(configFile, profileName);
            if (profile.isEmpty()) {
                out.writeError("Profile '" + profileName + "' not found.");
                out.writeLine("");
                out.writeLine("Available profiles:");

                for (OktaProfile p : profileService.listProfiles(configFile)) {
                    out.writeLine("  - " + p.getName());
                }

                out.writeLine("");
                out.writeLine("Use 'okta login --profile-name " + profileName + "' to create this profile.");
                return 1;
            }

            // Set as active
            profileService.setActiveProfile(configFile, profileName);

            out.writeLine("Switched to profile '" + profileName + "'");
            out.writeLine("Org URL: " + profile.get().getOrgUrl());
        }

        return 0;
    }
}
