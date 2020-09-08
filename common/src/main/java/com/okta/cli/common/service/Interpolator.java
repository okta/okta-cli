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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public interface Interpolator {

    String interpolate(String text, Map<String, String> context);

    Logger log = LoggerFactory.getLogger(Interpolator.class);

    default void interpolate(Path path, Map<String, String> context) throws IOException {

        // TODO - better way to detect binary files?
        String fileContent = null;
        try {
            fileContent = Files.readString(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            log.debug("skipping binary file: {}", path.getFileName());
            return;
        }
        String result = interpolate(fileContent, context);
        // save a write to disk if nothing changed
        if (fileContent == null || result == null || result.equals(fileContent)) {
            return;
        }
        try (OutputStreamWriter writer =
                 new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
            writer.write(result);
        }
    }

}
