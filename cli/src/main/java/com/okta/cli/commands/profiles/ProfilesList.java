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
import java.util.List;

@CommandLine.Command(name = "list",
        description = "List all configured profiles")
public class ProfilesList extends BaseCommand {

    @Override
    public int runCommand() throws Exception {
        ProfileConfigurationService profileService = new DefaultProfileConfigurationService();
        File configFile = getEnvironment().getOktaPropsFile();

        // Migrate legacy format if needed
        if (((DefaultProfileConfigurationService) profileService).isLegacyFormat(configFile)) {
            ((DefaultProfileConfigurationService) profileService).migrateFromLegacyFormat(configFile);
        }

        List<OktaProfile> profiles = profileService.listProfiles(configFile);
        String activeProfile = profileService.getActiveProfileName(configFile);

        try (ConsoleOutput out = getConsoleOutput()) {
            if (profiles.isEmpty()) {
                out.writeLine("No profiles configured. Run 'okta login' to create one.");
                return 0;
            }

            out.bold("Configured Okta Profiles:");
            out.writeLine("");
            out.writeLine("");

            // Calculate column widths
            int maxNameLen = profiles.stream().mapToInt(p -> p.getName().length()).max().orElse(10);
            maxNameLen = Math.max(maxNameLen, 10);

            // Header
            out.write(String.format("  %-" + maxNameLen + "s  %-40s  %s%n", "NAME", "ORG URL", "STATUS"));
            out.write(String.format("  %-" + maxNameLen + "s  %-40s  %s%n",
                    "-".repeat(maxNameLen), "-".repeat(40), "------"));

            for (OktaProfile profile : profiles) {
                boolean isActive = profile.getName().equals(activeProfile);
                String status = isActive ? "* active" : "";
                String marker = isActive ? "* " : "  ";

                if (isActive) {
                    out.bold(marker);
                    out.bold(String.format("%-" + maxNameLen + "s", profile.getName()));
                    out.write("  ");
                    out.writeLine(String.format("%-40s  %s", profile.getOrgUrl(), status));
                } else {
                    out.write(marker);
                    out.writeLine(String.format("%-" + maxNameLen + "s  %-40s  %s",
                            profile.getName(), profile.getOrgUrl(), status));
                }
            }

            out.writeLine("");
            out.writeLine("Use 'okta profiles use <name>' to switch profiles.");
            out.writeLine("Use 'okta --profile <name> <command>' for one-off commands.");
        }

        return 0;
    }
}
