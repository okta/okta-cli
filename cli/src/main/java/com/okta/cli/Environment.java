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

import com.okta.cli.common.model.OktaProfile;
import com.okta.cli.common.service.DefaultProfileConfigurationService;
import com.okta.cli.common.service.ProfileConfigurationService;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.DefaultPrompter;
import com.okta.cli.console.DisabledPrompter;
import com.okta.cli.console.Prompter;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class Environment {

    private final File baseDir = new File(System.getProperty("user.dir"));

    private final File oktaPropsFile = new File(System.getProperty("user.home"), ".okta/okta.yaml");

    private Prompter prompter;

    private ConsoleOutput consoleOutput;

    private boolean interactive = true;

    private boolean verbose = false;

    private boolean consoleColors = true;

    private String profile = null;

    private boolean profileActivated = false;

    public boolean isInteractive() {
        return interactive;
    }

    public Environment setInteractive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }

    public Environment setConsoleColors(boolean consoleColors) {
        this.consoleColors = consoleColors;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public File workingDirectory() {
        return baseDir;
    }

    public File getOktaPropsFile() {
        return oktaPropsFile;
    }

    /**
     * Gets the currently selected profile name.
     * Returns the profile set via --profile flag, or the active profile from config,
     * or "default" if no profile is configured.
     *
     * @return the profile name
     */
    public String getProfile() {
        if (profile != null) {
            return profile;
        }

        // Check environment variable
        String envProfile = System.getenv("OKTA_CLI_PROFILE");
        if (envProfile != null && !envProfile.isEmpty()) {
            return envProfile;
        }

        // Get active profile from config file
        try {
            ProfileConfigurationService profileService = new DefaultProfileConfigurationService();
            return profileService.getActiveProfileName(oktaPropsFile);
        } catch (IOException e) {
            return ProfileConfigurationService.DEFAULT_PROFILE_NAME;
        }
    }

    /**
     * Sets the profile to use for this session.
     * This overrides the active profile from configuration.
     *
     * @param profile the profile name
     * @return this Environment for chaining
     */
    public Environment setProfile(String profile) {
        this.profile = profile;
        this.profileActivated = false; // Reset so profile will be activated on next SDK call
        return this;
    }

    /**
     * Activates the current profile by setting system properties for the Okta SDK.
     * This method is idempotent - calling it multiple times has no additional effect.
     *
     * @throws IllegalStateException if the profile does not exist or cannot be loaded
     */
    public void activateProfile() {
        if (profileActivated) {
            return;
        }

        ProfileConfigurationService profileService = new DefaultProfileConfigurationService();

        try {
            // Migrate legacy format if needed
            if (((DefaultProfileConfigurationService) profileService).isLegacyFormat(oktaPropsFile)) {
                ((DefaultProfileConfigurationService) profileService).migrateFromLegacyFormat(oktaPropsFile);
            }

            String profileName = getProfile();
            Optional<OktaProfile> profileOpt = profileService.getProfile(oktaPropsFile, profileName);

            if (profileOpt.isPresent()) {
                profileService.activateProfileForSdk(profileOpt.get());
                profileActivated = true;
            }
            // If profile doesn't exist, let the SDK handle the error (might be using env vars)
        } catch (IOException e) {
            // If we can't read the config, let the SDK try its default behavior
            if (verbose) {
                System.err.println("Warning: Could not load profile configuration: " + e.getMessage());
            }
        }
    }

    public boolean isDemo() {
        return Boolean.parseBoolean(System.getenv("OKTA_CLI_DEMO"));
    }

    public Prompter prompter() {
        if (prompter == null) {
            prompter = interactive
                    ? new DefaultPrompter(getConsoleOutput())
                    : new DisabledPrompter();
        }
        return prompter;
    }

    public ConsoleOutput getConsoleOutput() {
        if (consoleOutput == null) {
            consoleOutput = ConsoleOutput.create(consoleColors);
        }

        return consoleOutput;
    }
}
