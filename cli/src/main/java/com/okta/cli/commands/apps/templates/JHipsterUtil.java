/*
 * Copyright 2021-Present Okta, Inc.
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
package com.okta.cli.commands.apps.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Utility class for figuring out which JHipster Generator is used. When a generator cannot be determined the fallback
 * is Spring.
 */
final class JHipsterUtil {

    enum JHipsterGenerator {
        QUARKUS,
        MICRONAUT,
        DEFAULT,
    }

    public static Logger logger = LoggerFactory.getLogger(JHipsterUtil.class);

    private JHipsterUtil() {}

    static JHipsterGenerator getGenerator() {
        File currentDir = new File(System.getProperty("user.dir", "")).getAbsoluteFile();

        if (currentDir.exists()) {
            File packageJsonFile = new File(currentDir, "package.json");
            if (packageJsonFile.exists()) {
                try {
                    // This is a bit ugly, we could regex this, but since we have two options to start with
                    // quarkus and the default (spring), we can just do a string contains for now
                    String packageJson = Files.readString(packageJsonFile.toPath());
                    if (packageJson.contains("generator-jhipster-quarkus")) {
                        return JHipsterGenerator.QUARKUS;
                    }else if (packageJson.contains("generator-jhipster-micronaut")) {
                        return JHipsterGenerator.MICRONAUT;
                    } // add other JHipster implementations here

                } catch (IOException e) {
                    // log the error, fallback to spring impl
                    logger.warn("Failed to parse: {}", packageJsonFile.getAbsolutePath(), e);
                }
            }
        }
        return JHipsterGenerator.DEFAULT;
    }
}
