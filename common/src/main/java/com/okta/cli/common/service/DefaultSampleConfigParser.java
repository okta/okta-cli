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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class DefaultSampleConfigParser implements SampleConfigParser {

    private static final String DOC_URL = "https://github.com/okta/okta-cli/wiki/Create-an-Okta-Start-Sample";

    public OktaSampleConfig parseConfig(File configFile, Map<String, String> context) throws IOException {

        if (!configFile.exists()) {
            throw new IllegalArgumentException("A required Okta sample's config file could not be found in this " +
                                               "project: '" + configFile + "', if you are creating a new sample, " +
                                               "more information can be found here: " + DOC_URL);
        }

        // NOTE this is not the most memory efficient way to do this, but this file is small
        // if we need something more complex we can do that later.
        String configFileContent = Files.readString(configFile.toPath().toAbsolutePath(), StandardCharsets.UTF_8);

        // filter the file
        configFileContent = new DefaultInterpolator().interpolate(configFileContent, context);

        // ignore unknown properties, so we can add additional features and not break older clients
        Representer representer = new Representer(new DumperOptions());
        representer.getPropertyUtils().setSkipMissingProperties(true);

        OktaSampleConfig config = new Yaml(new SafeConstructor(new LoaderOptions()), representer).loadAs(configFileContent, OktaSampleConfig.class);

        // TODO improve validation of configuration
        if (config.getOAuthClient() == null) {
            throw new IllegalArgumentException("Sample configuration file: '" + configFile.getAbsoluteFile() +
                                               "' must contain an 'oauthClient' element, see: " + DOC_URL);
        }
        return config;
    }
}
