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

import org.testng.annotations.Test

import static com.okta.cli.test.CommandRunner.resultMatches
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class HelpIT {

    @Test
    void noArgs() {
        def result = new CommandRunner().runCommand()
        assertThat result, resultMatches(2, emptyString(), startsWith("Specify a command\n"))
    }

    @Test
    void help() {
        def result = new CommandRunner().runCommand("help")
        assertThat result, resultMatches(0, allOf(
                containsString("\n  register "),
                containsString("\n  login "),
                containsString("\n  apps "),
                containsString("\n  help ")
        ), emptyString())
    }
}
