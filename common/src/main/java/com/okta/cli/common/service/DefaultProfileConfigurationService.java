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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of ProfileConfigurationService.
 *
 * Configuration file format:
 * <pre>
 * okta:
 *   profiles:
 *     default:
 *       orgUrl: https://dev-123456.okta.com
 *       token: 00abc...
 *     acme-corp:
 *       orgUrl: https://acme.okta.com
 *       token: 00xyz...
 *   activeProfile: default
 *   # Legacy format support (read-only, migrated on first write):
 *   client:
 *     orgUrl: https://dev-123456.okta.com
 *     token: 00abc...
 * </pre>
 */
public class DefaultProfileConfigurationService implements ProfileConfigurationService {

    private static final String OKTA_KEY = "okta";
    private static final String PROFILES_KEY = "profiles";
    private static final String ACTIVE_PROFILE_KEY = "activeProfile";
    private static final String ORG_URL_KEY = "orgUrl";
    private static final String TOKEN_KEY = "token";
    private static final String LEGACY_CLIENT_KEY = "client";

    @Override
    public List<OktaProfile> listProfiles(File configFile) throws IOException {
        Map<String, Object> config = loadConfig(configFile);
        Map<String, Map<String, String>> profiles = getProfilesMap(config);

        List<OktaProfile> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : profiles.entrySet()) {
            String name = entry.getKey();
            Map<String, String> profileData = entry.getValue();
            String orgUrl = profileData.get(ORG_URL_KEY);
            String token = profileData.get(TOKEN_KEY);
            if (orgUrl != null && token != null) {
                result.add(new OktaProfile(name, orgUrl, token));
            }
        }
        return result;
    }

    @Override
    public Optional<OktaProfile> getProfile(File configFile, String profileName) throws IOException {
        Map<String, Object> config = loadConfig(configFile);
        Map<String, Map<String, String>> profiles = getProfilesMap(config);

        Map<String, String> profileData = profiles.get(profileName);
        if (profileData == null) {
            return Optional.empty();
        }

        String orgUrl = profileData.get(ORG_URL_KEY);
        String token = profileData.get(TOKEN_KEY);
        if (orgUrl == null || token == null) {
            return Optional.empty();
        }

        return Optional.of(new OktaProfile(profileName, orgUrl, token));
    }

    @Override
    public String getActiveProfileName(File configFile) throws IOException {
        Map<String, Object> config = loadConfig(configFile);
        Map<String, Object> oktaConfig = getOktaConfig(config);

        Object activeProfile = oktaConfig.get(ACTIVE_PROFILE_KEY);
        if (activeProfile instanceof String && !((String) activeProfile).isEmpty()) {
            return (String) activeProfile;
        }
        return DEFAULT_PROFILE_NAME;
    }

    @Override
    public void saveProfile(File configFile, OktaProfile profile, boolean setAsActive) throws IOException {
        Map<String, Object> config = loadConfig(configFile);
        Map<String, Object> oktaConfig = getOrCreateOktaConfig(config);
        Map<String, Map<String, String>> profiles = getOrCreateProfilesMap(oktaConfig);

        // Create profile data
        Map<String, String> profileData = new LinkedHashMap<>();
        profileData.put(ORG_URL_KEY, profile.getOrgUrl());
        profileData.put(TOKEN_KEY, profile.getApiToken());
        profiles.put(profile.getName(), profileData);

        // Set as active if requested or if it's the first profile
        if (setAsActive || profiles.size() == 1) {
            oktaConfig.put(ACTIVE_PROFILE_KEY, profile.getName());
        }

        saveConfig(configFile, config);
    }

    @Override
    public void setActiveProfile(File configFile, String profileName) throws IOException {
        Map<String, Object> config = loadConfig(configFile);
        Map<String, Map<String, String>> profiles = getProfilesMap(config);

        if (!profiles.containsKey(profileName)) {
            throw new IllegalArgumentException("Profile '" + profileName + "' does not exist");
        }

        Map<String, Object> oktaConfig = getOrCreateOktaConfig(config);
        oktaConfig.put(ACTIVE_PROFILE_KEY, profileName);

        saveConfig(configFile, config);
    }

    @Override
    public boolean deleteProfile(File configFile, String profileName) throws IOException {
        Map<String, Object> config = loadConfig(configFile);
        Map<String, Object> oktaConfig = getOktaConfig(config);
        Map<String, Map<String, String>> profiles = getProfilesMap(config);

        // Check if trying to delete active profile
        String activeProfile = getActiveProfileName(configFile);
        if (profileName.equals(activeProfile)) {
            throw new IllegalArgumentException("Cannot delete the active profile '" + profileName + "'. Switch to another profile first.");
        }

        if (!profiles.containsKey(profileName)) {
            return false;
        }

        profiles.remove(profileName);
        saveConfig(configFile, config);
        return true;
    }

    @Override
    public void activateProfileForSdk(OktaProfile profile) {
        // Set system properties that the Okta SDK will pick up
        System.setProperty("okta.client.orgUrl", profile.getOrgUrl());
        System.setProperty("okta.client.token", profile.getApiToken());
    }

    /**
     * Migrates legacy configuration format to the new profiles format.
     * The legacy format stored credentials directly under okta.client.
     *
     * @param configFile the configuration file to migrate
     * @throws IOException if migration fails
     */
    public void migrateFromLegacyFormat(File configFile) throws IOException {
        Map<String, Object> config = loadConfig(configFile);
        Map<String, Object> oktaConfig = getOktaConfig(config);

        // Check if already migrated
        if (oktaConfig.containsKey(PROFILES_KEY)) {
            return;
        }

        // Check for legacy format
        @SuppressWarnings("unchecked")
        Map<String, String> legacyClient = (Map<String, String>) oktaConfig.get(LEGACY_CLIENT_KEY);
        if (legacyClient == null) {
            return;
        }

        String orgUrl = legacyClient.get(ORG_URL_KEY);
        String token = legacyClient.get(TOKEN_KEY);
        if (orgUrl == null || token == null) {
            return;
        }

        // Create default profile from legacy data
        OktaProfile defaultProfile = new OktaProfile(DEFAULT_PROFILE_NAME, orgUrl, token);
        saveProfile(configFile, defaultProfile, true);
    }

    /**
     * Checks if the configuration file uses the legacy format (single profile under okta.client).
     *
     * @param configFile the configuration file to check
     * @return true if using legacy format
     * @throws IOException if the file cannot be read
     */
    public boolean isLegacyFormat(File configFile) throws IOException {
        if (!configFile.exists()) {
            return false;
        }

        Map<String, Object> config = loadConfig(configFile);
        Map<String, Object> oktaConfig = getOktaConfig(config);

        // Legacy format has 'client' but no 'profiles'
        return oktaConfig.containsKey(LEGACY_CLIENT_KEY) && !oktaConfig.containsKey(PROFILES_KEY);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig(File configFile) throws IOException {
        if (!configFile.exists()) {
            return new LinkedHashMap<>();
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            Object loaded = yaml.load(inputStream);
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOktaConfig(Map<String, Object> config) {
        Object okta = config.get(OKTA_KEY);
        if (okta instanceof Map) {
            return (Map<String, Object>) okta;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateOktaConfig(Map<String, Object> config) {
        return (Map<String, Object>) config.computeIfAbsent(OKTA_KEY, k -> new LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> getProfilesMap(Map<String, Object> config) {
        Map<String, Object> oktaConfig = getOktaConfig(config);
        Object profiles = oktaConfig.get(PROFILES_KEY);
        if (profiles instanceof Map) {
            return (Map<String, Map<String, String>>) profiles;
        }

        // Check for legacy format and return it as a single "default" profile
        Object legacyClient = oktaConfig.get(LEGACY_CLIENT_KEY);
        if (legacyClient instanceof Map) {
            Map<String, Map<String, String>> legacyProfiles = new LinkedHashMap<>();
            legacyProfiles.put(DEFAULT_PROFILE_NAME, (Map<String, String>) legacyClient);
            return legacyProfiles;
        }

        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> getOrCreateProfilesMap(Map<String, Object> oktaConfig) {
        return (Map<String, Map<String, String>>) oktaConfig.computeIfAbsent(PROFILES_KEY, k -> new LinkedHashMap<>());
    }

    private void saveConfig(File configFile, Map<String, Object> config) throws IOException {
        File parentDir = configFile.getParentFile();

        // Create parent directory if needed
        if (parentDir != null && !(parentDir.exists() || parentDir.mkdirs())) {
            throw new IOException("Unable to create directory: " + parentDir.getAbsolutePath());
        }

        // Configure YAML dumper for readable output
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Yaml yaml = new Yaml(options);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            yaml.dump(config, writer);
        }

        // Set secure file permissions on POSIX systems
        Set<String> supportedViews = FileSystems.getDefault().supportedFileAttributeViews();
        if (supportedViews.contains("posix")) {
            if (parentDir != null) {
                Files.setPosixFilePermissions(parentDir.toPath(), Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE));
            }

            Files.setPosixFilePermissions(configFile.toPath(), Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        }
    }
}
