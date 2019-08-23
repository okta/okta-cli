/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.maven.orgcreation.service;

import com.google.auto.service.AutoService;
import com.okta.commons.lang.ApplicationInfo;
import com.okta.sdk.http.UserAgentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.Collections.list;

/**
 * @deprecated After the release of the Okta SDK 1.6 this class will no longer be needed, and the version metadata will be pulled in automatically.
 */
@Deprecated
@AutoService(UserAgentProvider.class)
public class MavenPluginUserAgentProvider implements UserAgentProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MavenPluginUserAgentProvider.class);
    private static final String VERSION_FILE_LOCATION = "META-INF/okta/version.properties";

    @Override
    public String getUserAgent() {
        return oktaComponentsFromVersionMetadata().entrySet().stream()
                .map(e -> e.getKey() + "/" + e.getValue())
                .collect(Collectors.joining(" "));
    }

    private static Map<String, String> oktaComponentsFromVersionMetadata() {
        Map<String, String> results = new HashMap<>();
        try {
            list(ApplicationInfo.class.getClassLoader().getResources(VERSION_FILE_LOCATION)).stream()
                    .map(MavenPluginUserAgentProvider::loadProps)
                    .forEach(properties -> results.putAll(entriesFromOktaVersionMetadata(properties)));
        } catch (IOException e) { //NOPMD
            // don't fail when gathering info
            LOG.warn("Failed to locate okta component version metadata as a resource: {}", VERSION_FILE_LOCATION);
        }
        return results;
    }

    private static Properties loadProps(URL resourceUrl) {
        try {
            Properties props = new Properties();
            props.load(resourceUrl.openStream());
            return props;
        } catch (IOException e) {
            // don't fail when gathering info
            LOG.warn("Failed to open properties file: '{}', but this file was detected on your classpath", resourceUrl);
        }
        return null;
    }

    private static Map<String, String> entriesFromOktaVersionMetadata(Properties properties) {

        if (properties == null) {
            return Collections.emptyMap();
        }

        return properties.entrySet().stream()
            .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
    }
}
