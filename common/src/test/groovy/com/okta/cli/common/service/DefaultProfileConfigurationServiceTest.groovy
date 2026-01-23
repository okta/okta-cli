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
package com.okta.cli.common.service

import com.okta.cli.common.TestUtil
import com.okta.cli.common.model.OktaProfile
import org.testng.annotations.Test

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class DefaultProfileConfigurationServiceTest {

    @Test
    void saveAndLoadProfile() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        OktaProfile profile = new OktaProfile("default", "https://dev-123456.okta.com", "test-token-123")
        service.saveProfile(configFile, profile, true)

        // Verify profile was saved
        Optional<OktaProfile> loaded = service.getProfile(configFile, "default")
        assertThat loaded.isPresent(), is(true)
        assertThat loaded.get().getName(), is("default")
        assertThat loaded.get().getOrgUrl(), is("https://dev-123456.okta.com")
        assertThat loaded.get().getApiToken(), is("test-token-123")
    }

    @Test
    void listProfiles() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        service.saveProfile(configFile, new OktaProfile("default", "https://default.okta.com", "token1"), true)
        service.saveProfile(configFile, new OktaProfile("acme-corp", "https://acme.okta.com", "token2"), false)
        service.saveProfile(configFile, new OktaProfile("bigco-prod", "https://bigco.okta.com", "token3"), false)

        List<OktaProfile> profiles = service.listProfiles(configFile)
        assertThat profiles.size(), is(3)
        assertThat profiles*.name, containsInAnyOrder("default", "acme-corp", "bigco-prod")
    }

    @Test
    void setActiveProfile() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        service.saveProfile(configFile, new OktaProfile("profile1", "https://p1.okta.com", "token1"), true)
        service.saveProfile(configFile, new OktaProfile("profile2", "https://p2.okta.com", "token2"), false)

        assertThat service.getActiveProfileName(configFile), is("profile1")

        service.setActiveProfile(configFile, "profile2")
        assertThat service.getActiveProfileName(configFile), is("profile2")
    }

    @Test
    void deleteProfile() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        service.saveProfile(configFile, new OktaProfile("active", "https://active.okta.com", "token1"), true)
        service.saveProfile(configFile, new OktaProfile("to-delete", "https://delete.okta.com", "token2"), false)

        assertThat service.profileExists(configFile, "to-delete"), is(true)

        boolean deleted = service.deleteProfile(configFile, "to-delete")
        assertThat deleted, is(true)
        assertThat service.profileExists(configFile, "to-delete"), is(false)
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void cannotDeleteActiveProfile() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        service.saveProfile(configFile, new OktaProfile("active", "https://active.okta.com", "token1"), true)
        service.deleteProfile(configFile, "active")
    }

    @Test
    void migrateFromLegacyFormat() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        // Write legacy format
        configFile.text = """
okta:
  client:
    orgUrl: https://legacy.okta.com
    token: legacy-token
"""

        assertThat service.isLegacyFormat(configFile), is(true)

        // Migrate
        service.migrateFromLegacyFormat(configFile)

        // Verify migration
        assertThat service.isLegacyFormat(configFile), is(false)
        Optional<OktaProfile> profile = service.getProfile(configFile, "default")
        assertThat profile.isPresent(), is(true)
        assertThat profile.get().getOrgUrl(), is("https://legacy.okta.com")
        assertThat profile.get().getApiToken(), is("legacy-token")
    }

    @Test
    void legacyFormatReadAsDefault() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        // Write legacy format
        configFile.text = """
okta:
  client:
    orgUrl: https://legacy.okta.com
    token: legacy-token
"""

        // Can read legacy format as "default" profile without migration
        List<OktaProfile> profiles = service.listProfiles(configFile)
        assertThat profiles.size(), is(1)
        assertThat profiles[0].name, is("default")
        assertThat profiles[0].orgUrl, is("https://legacy.okta.com")
    }

    @Test
    void activateProfileForSdk() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        OktaProfile profile = new OktaProfile("test", "https://test.okta.com", "sdk-token")

        // Clear any existing properties
        System.clearProperty("okta.client.orgUrl")
        System.clearProperty("okta.client.token")

        service.activateProfileForSdk(profile)

        assertThat System.getProperty("okta.client.orgUrl"), is("https://test.okta.com")
        assertThat System.getProperty("okta.client.token"), is("sdk-token")

        // Cleanup
        System.clearProperty("okta.client.orgUrl")
        System.clearProperty("okta.client.token")
    }

    @Test
    void emptyConfigFile() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")
        // File doesn't exist yet

        List<OktaProfile> profiles = service.listProfiles(configFile)
        assertThat profiles.size(), is(0)
        assertThat service.getActiveProfileName(configFile), is("default")
    }

    @Test
    void filePermissions() {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return // Skip on non-POSIX systems
        }

        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")

        service.saveProfile(configFile, new OktaProfile("test", "https://test.okta.com", "token"), true)

        assertThat Files.getPosixFilePermissions(configFile.toPath()), is([
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE] as Set)
    }

    @Test
    void profileNotFound() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")
        service.saveProfile(configFile, new OktaProfile("default", "https://test.okta.com", "token"), true)

        Optional<OktaProfile> profile = service.getProfile(configFile, "nonexistent")
        assertThat profile.isPresent(), is(false)
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void setActiveProfileNotFound() {
        DefaultProfileConfigurationService service = new DefaultProfileConfigurationService()
        File configFile = new File(File.createTempDir("profile-test-", "-dir"), "okta.yaml")
        service.saveProfile(configFile, new OktaProfile("default", "https://test.okta.com", "token"), true)

        service.setActiveProfile(configFile, "nonexistent")
    }
}
