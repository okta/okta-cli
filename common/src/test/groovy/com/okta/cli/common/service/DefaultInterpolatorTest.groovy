/*
 * Copyright 2022-Present Okta, Inc.
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
package com.okta.cli.common.service

import com.google.common.jimfs.Jimfs
import com.google.common.jimfs.Configuration
import org.testng.annotations.Test

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

import static java.nio.charset.StandardCharsets.UTF_8
import static java.nio.file.Files.write
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Mockito.spy
import static org.mockito.Mockito.when

class DefaultInterpolatorTest {

    private final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix())

    @Test
    void basicInterpolation() {
        Path path = createTestFile()
        new DefaultInterpolator().interpolate(path, ["foo": "bar"])

        String result = Files.readString(path, UTF_8)
        assertThat(result, equalTo("file contents bar"))
    }

    @Test
    void fileOpenException() {
        Path path = createTestFile()
        Interpolator interpolator = spy(new DefaultInterpolator())
        when(interpolator.readFile(path)).thenThrow(new IOException("Test exception"))

        // does not throw an exception
        interpolator.interpolate(path, ["foo": "bar"])
    }

    Path path(String filename) {
        return fileSystem.getPath(filename)
    }

    Path createTestFile() {
        Path path = path("/foo")
        write(path, 'file contents ${foo}'.getBytes(UTF_8))
        return path
    }
}
