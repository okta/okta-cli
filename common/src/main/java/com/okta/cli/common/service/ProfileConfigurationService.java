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
package com.okta.cli.common.service;

import com.okta.cli.common.model.OktaProfile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing multiple Okta CLI profiles.
 * Profiles allow users to switch between different Okta organizations easily.
 */
public interface ProfileConfigurationService {

    /**
     * Default profile name used when no profile is specified.
     */
    String DEFAULT_PROFILE_NAME = "default";

    /**
     * Gets all configured profiles.
     *
     * @param configFile the configuration file to read from
     * @return list of all configured profiles
     * @throws IOException if the file cannot be read
     */
    List<OktaProfile> listProfiles(File configFile) throws IOException;

    /**
     * Gets a specific profile by name.
     *
     * @param configFile the configuration file to read from
     * @param profileName the name of the profile to retrieve
     * @return the profile if found, empty otherwise
     * @throws IOException if the file cannot be read
     */
    Optional<OktaProfile> getProfile(File configFile, String profileName) throws IOException;

    /**
     * Gets the currently active profile name.
     *
     * @param configFile the configuration file to read from
     * @return the active profile name, defaults to "default" if not set
     * @throws IOException if the file cannot be read
     */
    String getActiveProfileName(File configFile) throws IOException;

    /**
     * Saves or updates a profile configuration.
     *
     * @param configFile the configuration file to write to
     * @param profile the profile to save
     * @param setAsActive whether to set this profile as the active profile
     * @throws IOException if the file cannot be written
     */
    void saveProfile(File configFile, OktaProfile profile, boolean setAsActive) throws IOException;

    /**
     * Sets the active profile.
     *
     * @param configFile the configuration file to write to
     * @param profileName the name of the profile to make active
     * @throws IOException if the file cannot be written
     * @throws IllegalArgumentException if the profile does not exist
     */
    void setActiveProfile(File configFile, String profileName) throws IOException;

    /**
     * Deletes a profile.
     *
     * @param configFile the configuration file to modify
     * @param profileName the name of the profile to delete
     * @return true if the profile was deleted, false if it didn't exist
     * @throws IOException if the file cannot be written
     * @throws IllegalArgumentException if attempting to delete the active profile
     */
    boolean deleteProfile(File configFile, String profileName) throws IOException;

    /**
     * Activates a profile by setting the appropriate system properties.
     * This allows the Okta SDK to pick up the profile's credentials.
     *
     * @param profile the profile to activate
     */
    void activateProfileForSdk(OktaProfile profile);

    /**
     * Checks if a profile exists.
     *
     * @param configFile the configuration file to read from
     * @param profileName the name of the profile to check
     * @return true if the profile exists
     * @throws IOException if the file cannot be read
     */
    default boolean profileExists(File configFile, String profileName) throws IOException {
        return getProfile(configFile, profileName).isPresent();
    }
}
