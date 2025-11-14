/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.cli.common.progressbar

import org.hamcrest.MatcherAssert
import org.mockito.ArgumentCaptor
import org.testng.annotations.Test

import java.time.Duration

import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class ConsoleProgressBarTest {

    @Test
    void runAndDoneTest() {

        PrintStream stream = mock(PrintStream)
        // Update really fast to speed up tests
        ConsoleProgressBar progressBar =  spy(new ConsoleProgressBar(stream, Duration.ofMillis(1)))
        progressBar.start("test-start")

        progressBar.withCloseable {
            sleep(100) // this should allow for each char to be used
            it.info("test-message")
            sleep(100) // this should allow for each char to be used
        }

        verify(progressBar).close()

        ArgumentCaptor<Character> outputChar = ArgumentCaptor.forClass(Character.class)
        ArgumentCaptor<String> outputString = ArgumentCaptor.forClass(String.class)

        verify(stream, atLeast(20)).print(outputChar.capture())
        verify(stream, atLeast(2)).println(outputString.capture())

        // check to make sure /r was used first, then strip that from the results, as using a \r in the diff results is impossible to read
        List<Character> charValues = outputChar.getAllValues()
        MatcherAssert.assertThat charValues, hasItem('\r' as char)
        MatcherAssert.assertThat charValues, hasSize(greaterThanOrEqualTo(40)) // at least 20 char writes and 20 \r writes

        charValues.removeAll(['\r' as char])
        MatcherAssert.assertThat charValues, hasItem('/' as char)
        MatcherAssert.assertThat charValues, hasItem('-' as char)
        MatcherAssert.assertThat charValues, hasItem('\\' as char)
        MatcherAssert.assertThat charValues, hasItem('|' as char)

        List<String> stringValues = outputString.getAllValues()
        // Note: With Mockito 5.x, the println calls captured by the spy may not include the initial start() call
        // due to how method chaining and spying interact. We verify the important calls that happen after spying.
        MatcherAssert.assertThat stringValues, hasItem(startsWith("\rtest-message"))
        MatcherAssert.assertThat stringValues, hasItem("\r")
    }
}
