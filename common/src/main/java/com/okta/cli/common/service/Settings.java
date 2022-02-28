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

class Settings {

    /**
     * The base URL of the service used to create a new Okta account.
     * This value is NOT exposed as a plugin parameter, but CAN be set using the env var {@code OKTA_CLI_REGISTRATION_URL}.
     */
    private static final String DEFAULT_CLI_API_URL = "https://start.okta.dev/";
    private static final String DEFAULT_REGISTRATION_BASE_URL = "https://okta-devok12.okta.com/";
    private static final String DEFAULT_REGISTRATION_ID = "reg405abrRAkn0TRf5d6";

    static String getProperty(String envVar, String systemProperty, String defaultValue) {
        // Resolve baseURL via ENV Var, System property, and fallback to the default
        return System.getenv().getOrDefault(envVar, // check env var first
                System.getProperties().getProperty(systemProperty, // try system property
                        defaultValue)); // fallback to default value
    }

    static String getRegistrationBaseUrl() {
        return getProperty("OKTA_CLI_REGISTRATION_URL", "okta.cli.registrationUrl", DEFAULT_REGISTRATION_BASE_URL);
    }

    static String getRegistrationId() {
        return getProperty("OKTA_CLI_REGISTRATION_ID", "okta.cli.registrationId", DEFAULT_REGISTRATION_ID);
    }

    static String getCliApiUrl() {
        return getProperty("OKTA_CLI_API_URL", "okta.cli.apiUrl", DEFAULT_CLI_API_URL);
    }
}
