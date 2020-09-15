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

public interface SampleConfigParser {

    String SAMPLE_CONFIG_PATH = ".okta/okta.yaml";

    default OktaSampleConfig loadConfig() throws IOException {
        return parseConfig(new File(SAMPLE_CONFIG_PATH));
    }

    default OktaSampleConfig loadConfig(File localPath) throws IOException {
        return parseConfig(new File(localPath, SAMPLE_CONFIG_PATH));
    }

    OktaSampleConfig parseConfig(File configFile) throws IOException;
}