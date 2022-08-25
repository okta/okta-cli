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

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface Interpolator {

    String interpolate(String text, Map<String, String> context);

    Logger log = LoggerFactory.getLogger(Interpolator.class);

    default void interpolate(Path path, Map<String, String> context) throws IOException {

        // TODO - better way to detect binary files?
        String fileContent;
        try {
            fileContent = readFile(path);
        } catch (MalformedInputException e) {
            log.debug("skipping binary file: {}", path.getFileName());
            return;
        } catch (IOException e) {
            log.warn("Failed to read file: {}", path.getFileName());
            return;
        }
        String result = interpolate(fileContent, context);
        // save a write to disk if nothing changed
        if (fileContent == null || result == null || result.equals(fileContent)) {
            return;
        }

        writeFile(path, result);
    }

    // allows for testing
    default String readFile(Path path) throws IOException {
        return Files.readString(path, UTF_8);
    }

    // allows for testing
    default void writeFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(UTF_8));
    }
}
