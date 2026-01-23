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

@CommandLine.Command(name = "show",
        description = "Show details of a profile")
public class ProfilesShow extends BaseCommand {

    @CommandLine.Parameters(index = "0", description = "The profile name to show (defaults to active profile)",
            defaultValue = "")
    private String profileName;

    @Override
    public int runCommand() throws Exception {
        ProfileConfigurationService profileService = new DefaultProfileConfigurationService();
        File configFile = getEnvironment().getOktaPropsFile();

        // Use active profile if not specified
        String targetProfile = profileName.isEmpty()
                ? profileService.getActiveProfileName(configFile)
                : profileName;

        try (ConsoleOutput out = getConsoleOutput()) {
            Optional<OktaProfile> profile = profileService.getProfile(configFile, targetProfile);
            if (profile.isEmpty()) {
                out.writeError("Profile '" + targetProfile + "' not found.");
                return 1;
            }

            OktaProfile p = profile.get();
            String activeProfile = profileService.getActiveProfileName(configFile);
            boolean isActive = p.getName().equals(activeProfile);

            out.bold("Profile: ");
            out.writeLine(p.getName() + (isActive ? " (active)" : ""));
            out.bold("Org URL: ");
            out.writeLine(p.getOrgUrl());
            out.bold("Token:   ");
            // Mask the token for security
            String maskedToken = maskToken(p.getApiToken());
            out.writeLine(maskedToken);
        }

        return 0;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
