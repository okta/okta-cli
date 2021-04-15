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
package com.okta.cli.test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.utils.IOUtils
import org.testng.annotations.Test

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString

class StartIT implements MockWebSupport, CreateAppSupport {

    @Test
    void regAndListSamples() {
        // Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.INFO)


        MockWebServer mockWebServer = createMockServer()

        def orgUrl = url(mockWebServer, "/")

        List<MockResponse> responses = [
                // reg requests
                jsonRequest('{ "orgUrl": "' + orgUrl + '", "email": "test-email@example.com", "developerOrgCliToken": "test-id" }'),
                jsonRequest('{ "orgUrl": "' + orgUrl + '", "email": "test-email@example.com", "apiToken": "fake-test-token", "status": "ACTIVE" }'),

                // list samples
                jsonRequest([items: [
                        [name: "project-a", description: "a test description", tarballUrl: url(mockWebServer, "/tarball/okta-project-a-sample")],
                        [name: "test-project", description: "test description", tarballUrl: url(mockWebServer, "/tarball/okta-test-project-sample")]
                ]]),

                // download project zip
                new MockResponse()
                        .setResponseCode(200)
                        .setBody(tarDir())
                        .setHeader("Content-Type", "application/x-gzip"),

                // Setting up OIDC app
                // GET /api/v1/authorizationServers
                jsonRequest('[{ "id": "test-as", "name": "test-as-name", "issuer": "' + url(mockWebServer,"/") + '/oauth2/test-as" }]'),
                // GET /api/v1/apps?q=integration-tests
                jsonRequest('[]'),
                // POST /api/v1/apps
                jsonRequest('{ "id": "test-app-id", "label": "test-app-name" }'),
                // GET /api/v1/groups?q=everyone
                jsonRequest("[${everyoneGroup()}]"),
                // PUT /api/v1/apps/test-app-id/groups/every1-id
                jsonRequest('{}'),
                //GET /api/v1/internal/apps/test-app-id/settings/clientcreds
                jsonRequest('{ "client_id": "test-id" }')
        ]

        mockWebServer.with {
            responses.forEach { mockWebServer.enqueue(it) }

            List<String> input = [
                    // reg inputs
                    "test-first",
                    "test-last",
                    "test-email@example.com",
                    "Petoria",
                    "", // accept prompt to continue
                    // select a sample
                    "2"
            ]

            def result = new CommandRunner(url(mockWebServer, "/")).runCommandWithInput(input, "start")
            assertThat result, resultMatches(0, allOf(
                        // registration
                        containsString("An account activation email has been sent to you."),
                        containsString("Select a sample"),
                        containsString("a test description"),
                        containsString("Change the directory:"),
                        containsString("cd okta-test-project-sample"),
                        containsString("Some simple directions")
                    ),
                    null)
        }
    }

    private Buffer tarDir(String name = "test-project") {

        Buffer buffer = new Buffer()
        buffer.outputStream().with {
            TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(it))

            String classesDir = getClass().getProtectionDomain().getCodeSource().getLocation().getFile()
            def projectDir = new File(classesDir, "samples/${name}").toPath()

            Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
                @Override
                FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    File file = path.toFile()
                    String regex = ".*/samples/"
                    if (File.separator != "/") { // check for Windows
                        regex = ".*\\\\samples\\\\"
                    }
                    ArchiveEntry entry = tar.createArchiveEntry(file, file.getPath().replaceAll(regex, ""))
                    tar.putArchiveEntry(entry)
                    new FileInputStream(file).with {
                        IOUtils.copy(it, tar)
                    }
                    tar.closeArchiveEntry()
                    return FileVisitResult.CONTINUE
                }
            })
            tar.close()
        }

        return buffer
    }
}
