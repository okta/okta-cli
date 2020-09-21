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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DefaultSampleConfigParser implements SampleConfigParser {

    @Override
    public OktaSampleConfig parseConfig(File configFile) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(configFile.getAbsoluteFile())) {

            // ignore unknown properties, so we can add additional features and not break older clients
            Representer representer = new Representer();
            representer.getPropertyUtils().setSkipMissingProperties(true);
            OktaSampleConfig config = new Yaml(representer).loadAs(fileInputStream, OktaSampleConfig.class);

            // TODO improve validation of configuration
            if (config.getOAuthClient() == null) {
                throw new IllegalArgumentException("Sample configuration file: '" + configFile.getAbsoluteFile() +
                                                   "' must contain an 'oauthClient' element, see: " +
                                                   "https://github.com/oktadeveloper/okta-cli/wiki/Create-an-Okta-Start-Samples");
            }
            return config;
        }
    }
}
