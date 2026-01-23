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
package com.okta.cli.commands;

import com.okta.cli.common.model.OktaProfile;
import com.okta.cli.common.service.DefaultProfileConfigurationService;
import com.okta.cli.common.service.ProfileConfigurationService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.commons.configcheck.ConfigurationValidator;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;

@CommandLine.Command(name = "login",
        description = "Authorizes the Okta CLI tool")
public class Login extends BaseCommand {

    @CommandLine.Option(names = {"--profile-name"}, description = "Name for this profile (e.g., 'acme-corp', 'dev-tenant')")
    private String profileName;

    @Override
    public int runCommand() throws Exception {

        ProfileConfigurationService profileService = new DefaultProfileConfigurationService();
        File configFile = getEnvironment().getOktaPropsFile();

        // Migrate legacy format if needed
        if (((DefaultProfileConfigurationService) profileService).isLegacyFormat(configFile)) {
            ((DefaultProfileConfigurationService) profileService).migrateFromLegacyFormat(configFile);
        }

        // Determine profile name: --profile-name flag > --profile flag > prompt
        String targetProfile = profileName;
        if (targetProfile == null) {
            String envProfile = getEnvironment().getProfile();
            if (!ProfileConfigurationService.DEFAULT_PROFILE_NAME.equals(envProfile)) {
                targetProfile = envProfile;
            }
        }

        try (ConsoleOutput out = getConsoleOutput()) {
            // Prompt for profile name if not provided
            if (targetProfile == null) {
                targetProfile = getPrompter().promptUntilIfEmpty(null, "Profile name", ProfileConfigurationService.DEFAULT_PROFILE_NAME);
            }

            // Check if profile already exists
            Optional<OktaProfile> existingProfile = profileService.getProfile(configFile, targetProfile);
            if (existingProfile.isPresent()) {
                out.writeLine("Profile '" + targetProfile + "' already exists with org: " + existingProfile.get().getOrgUrl());
                if (!getPrompter().promptYesNo("Overwrite this profile?")) {
                    return 0;
                }
            }

            // Prompt for Okta Org URL
            String orgUrl = getPrompter().promptUntilValue("Okta Org URL");
            ConfigurationValidator.assertOrgUrl(orgUrl);

            // Prompt for API token
            out.writeLine("Enter your Okta API token, for more information see: https://bit.ly/get-okta-api-token");
            String apiToken = getPrompter().promptUntilValue(null, "Okta API token");
            ConfigurationValidator.assertApiToken(apiToken);

            // Determine if this should be the active profile
            boolean setAsActive = true;
            String currentActive = profileService.getActiveProfileName(configFile);
            if (!currentActive.equals(targetProfile) && profileService.profileExists(configFile, currentActive)) {
                setAsActive = getPrompter().promptYesNo("Set '" + targetProfile + "' as the active profile?", true);
            }

            // Save the profile
            OktaProfile newProfile = new OktaProfile(targetProfile, orgUrl, apiToken);
            profileService.saveProfile(configFile, newProfile, setAsActive);

            out.writeLine("");
            out.bold("Profile '" + targetProfile + "' saved successfully!");
            out.writeLine("");
            out.writeLine("Org URL: " + orgUrl);
            if (setAsActive) {
                out.writeLine("This profile is now active.");
            } else {
                out.writeLine("Use 'okta --profile " + targetProfile + " <command>' or 'okta profiles use " + targetProfile + "' to switch.");
            }
        }

        return 0;
    }
}
