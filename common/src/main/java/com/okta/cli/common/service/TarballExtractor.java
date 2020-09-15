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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * Extracts files from a tarball stripping the top level directory.
 */
public class TarballExtractor implements Extractor {

    @Override
    public void extract(String uri, File targetDirectory) throws IOException {
        try (TarArchiveInputStream zipStream = new TarArchiveInputStream(new GzipCompressorInputStream(get(uri)))) {
            TarArchiveEntry entry;

            while ((entry = zipStream.getNextTarEntry()) != null) {
                if (!zipStream.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }

                File destFile = fileName(targetDirectory, entry);
                if (entry.isDirectory()) {
                    if (!destFile.isDirectory() && !destFile.mkdirs()) {
                        throw new IOException("failed to create directory " + destFile);
                    }
                } else {
                    File parent = destFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }

                    int mode = entry.getMode();
                    try (OutputStream o = Files.newOutputStream(destFile.toPath())) {
                        IOUtils.copy(zipStream, o);
                    }
                    Files.setPosixFilePermissions(destFile.toPath(), permissionsFromMode(mode));
                    Files.setLastModifiedTime(destFile.toPath(), FileTime.from(entry.getLastModifiedDate().toInstant()));
                }
            }
        }
    }

    private InputStream get(String url) throws IOException {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);

        HttpResponse response = httpClient.execute(httpGet);

        // check for error
        if (response.getStatusLine().getStatusCode() == 200) {
            return response.getEntity().getContent();
        }

        throw new IOException("Failed to download: " + response.getStatusLine() + " - " + url);
    }

    private File fileName(File targetDir, ArchiveEntry archiveEntry) throws IOException {

        String name = archiveEntry.getName().replaceFirst("[^/]+/", "/");
        return FileUtils.ensureRelative(targetDir, name);
    }

    public static Set<PosixFilePermission> permissionsFromMode(int mode) {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        addPermissions(permissions, "OTHERS", mode);
        addPermissions(permissions, "GROUP", mode >> 3);
        addPermissions(permissions, "OWNER", mode >> 6);
        return permissions;
    }

    private static void addPermissions(Set<PosixFilePermission> permissions,
                                       String prefix, long mode) {
        if ((mode & 1) == 1) {
            permissions.add(PosixFilePermission.valueOf(prefix + "_EXECUTE"));
        }
        if ((mode & 2) == 2) {
            permissions.add(PosixFilePermission.valueOf(prefix + "_WRITE"));
        }
        if ((mode & 4) == 4) {
            permissions.add(PosixFilePermission.valueOf(prefix + "_READ"));
        }
    }
}
