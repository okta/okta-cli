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

import java.io.File;
import java.io.IOException;

public final class FileUtils {
    private FileUtils() {}

    public static File ensureRelative(File baseDir, String child) throws IOException {
        File canonicalDestinationDir = baseDir.getCanonicalFile();
        String canonicalDestinationPath = canonicalDestinationDir.getAbsolutePath();

        File destinationFile = new File(canonicalDestinationDir, child);
        String canonicalDestinationFile = destinationFile.getCanonicalPath();
        if (!(canonicalDestinationFile.startsWith(canonicalDestinationPath + File.separator)
              || canonicalDestinationFile.equals(canonicalDestinationPath))) {
            throw new IOException("Relative file " + child +" is outside the expected base directory: " + canonicalDestinationPath );
        }
        return destinationFile;
    }
}
