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

import com.okta.cli.common.model.OktaSampleConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static java.util.Collections.emptyMap;

public interface SampleConfigParser {

    String SAMPLE_CONFIG_PATH = ".okta/sample-config.yaml";

    default OktaSampleConfig loadConfig(File localPath) throws IOException {
        return parseConfig(new File(localPath, SAMPLE_CONFIG_PATH));
    }

    default OktaSampleConfig loadConfig(File localPath, Map<String, String> context) throws IOException {
        return parseConfig(new File(localPath, SAMPLE_CONFIG_PATH), context);
    }

    default OktaSampleConfig parseConfig(File configFile) throws IOException {
        return parseConfig(configFile, emptyMap());
    }

    OktaSampleConfig parseConfig(File configFile, Map<String, String> context) throws IOException;
}