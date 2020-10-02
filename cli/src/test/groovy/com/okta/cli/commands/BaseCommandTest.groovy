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
package com.okta.cli.commands

import com.okta.cli.OktaCli
import com.okta.cli.common.model.Semver
import com.okta.cli.common.model.VersionInfo
import com.okta.cli.common.service.StartRestClient
import com.okta.cli.console.ConsoleOutput
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.testng.annotations.Test

import java.time.Duration

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.emptyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class BaseCommandTest {

    @Test
    void notifyNewVersionTest() {

        def baos = new ByteArrayOutputStream()

        def command = new StubCommand(baos)
        command.call()

        def output = baos.toString()
        assertThat output, containsString("A new version of the Okta CLI is available: 1.2.3")
        assertThat output, containsString("See what's new: https://example.com/release/1.2.3")
    }

    @Test
    void failVersionFetch() {

        def baos = new ByteArrayOutputStream()
        def restClient = mock(StartRestClient)
        when(restClient.getVersionInfo()).thenThrow(new RuntimeException("expected test exception"))

        def command = new StubCommand(baos, restClient)
        command.call()

        def output = baos.toString()
        assertThat output, emptyString()
    }

    @Test(timeOut = 4000l)
    void versionTimeout() {

        def baos = new ByteArrayOutputStream()
        def restClient = mock(StartRestClient)
        when(restClient.getVersionInfo()).thenThrow(new RuntimeException("expected test exception"))

        def command = new StubCommand(baos, StubCommand.mockRestClient("1.2.3", Duration.ofSeconds(3)))
        command.call()

        def output = baos.toString()
        assertThat "Expected version thread to timeout, the result is no version info is displayed to the user", output, emptyString()
    }

    static class StubCommand extends BaseCommand {

        private final int exitCode
        private final String currentVersion

        StubCommand(ByteArrayOutputStream baos, StartRestClient restClient = mockRestClient("1.2.3", Duration.ofMillis(0)), OktaCli.StandardOptions standardOptions = new OktaCli.StandardOptions(), int exitCode = 0, String currentVersion = "1.0.1") {
            super(restClient, standardOptions)
            this.exitCode = exitCode
            this.currentVersion = currentVersion

            PrintStream printStream = new PrintStream(baos)
            ConsoleOutput out = new ConsoleOutput.AnsiConsoleOutput(printStream, false)
            getEnvironment().consoleOutput = out
        }

        @Override
        protected int runCommand() throws Exception {
            return exitCode
        }

        @Override
        Semver getCurrentVersion() {
            return Semver.parse(currentVersion)
        }

        static StartRestClient mockRestClient(String version, Duration delay) {
            def restClient = mock(StartRestClient)
            when(restClient.getVersionInfo()).thenAnswer(new Answer<VersionInfo>() {
                @Override
                VersionInfo answer(InvocationOnMock invocation) throws Throwable {
                    Thread.sleep(delay.toMillis())
                    return new VersionInfo()
                            .setLatestVersion(version)
                            .setLatestReleaseUrl("https://example.com/release/${version}")
                }
            })
            return restClient
        }
    }
}