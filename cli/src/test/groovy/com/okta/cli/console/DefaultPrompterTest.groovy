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
package com.okta.cli.console

import org.testng.Assert
import org.testng.annotations.Listeners
import org.testng.annotations.Test

import java.rmi.StubNotFoundException

import static org.hamcrest.Matchers.nullValue
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

@Listeners(RestoreSystemInOut)
class DefaultPrompterTest {

    @Test(timeOut = 1000l)
    void basicPrompter() {

        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("test-result")
        DefaultPrompter prompter = new DefaultPrompter(out)
        String result = prompter.prompt("hello")

        assertThat(result, equalTo("test-result"))
        verify(out).write("hello: ")
    }

    @Test(timeOut = 1000l)
    void nullResult() {

        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("")
        DefaultPrompter prompter = new DefaultPrompter(out)
        String result = prompter.prompt("hello")

        assertThat(result, nullValue())
    }

    @Test(timeOut = 1000l)
    void promptWithOptions_noDefault() {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ConsoleOutput out = new ConsoleOutput.AnsiConsoleOutput(new PrintStream(outputStream), true)

        expectInput("2")
        DefaultPrompter prompter = new DefaultPrompter(out)
        String result = prompter.prompt("hello", [new StubPromptOption("one", "one-1"), new StubPromptOption("two", "two-2")], null)

        // ansi colors
        String expectedOutput = "hello\n" +
                "\u001B[1m> 1: \u001B[0mone\n" +
                "\u001B[1m> 2: \u001B[0mtwo\n" +
                "Enter your choice: "

        assertThat(result, equalTo("two-2"))
        assertThat(outputStream.toString(), equalTo(expectedOutput))
    }

    @Test(timeOut = 1000l)
    void promptWithOptions_withDefault() {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ConsoleOutput out = new ConsoleOutput.AnsiConsoleOutput(new PrintStream(outputStream), true)

        expectInput("2")
        DefaultPrompter prompter = new DefaultPrompter(out)

        def options = [new StubPromptOption("one", "one-1"), new StubPromptOption("two", "two-2")]
        String result = prompter.prompt("hello", options, options[1])

        // ansi colors
        String expectedOutput = "hello\n" +
                "\u001B[1m> 1: \u001B[0mone\n" +
                "\u001B[1m> 2: \u001B[0mtwo\n" +
                "Enter your choice [two]: "

        assertThat(result, equalTo("two-2"))
        assertThat(outputStream.toString(), equalTo(expectedOutput))
    }

    @Test//(timeOut = 1000l)
    void promptWithOptions_invalidSelection() {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ConsoleOutput out = new ConsoleOutput.AnsiConsoleOutput(new PrintStream(outputStream), true)

        expectInput("3")
        DefaultPrompter prompter = new DefaultPrompter(out)

        def options = [new StubPromptOption("one", "one-1"), new StubPromptOption("two", "two-2")]
        String result = prompter.prompt("hello", options, options[1])

        // ansi colors
        String expectedOutput = "hello\n" +
                "\u001B[1m> 1: \u001B[0mone\n" +
                "\u001B[1m> 2: \u001B[0mtwo\n" +
                "Enter your choice [two]: \u001B[0;31m\n" +
                "Invalid choice, try again\n" +
                "\n" +
                "\u001B[0mhello\n" +
                "\u001B[1m> 1: \u001B[0mone\n" +
                "\u001B[1m> 2: \u001B[0mtwo\n" +
                "Enter your choice [two]: "

        assertThat(result, equalTo("two-2"))
        assertThat(outputStream.toString(), equalTo(expectedOutput))
    }

    @Test(timeOut = 1000l)
    void failToReadLine() {
        ConsoleOutput out = mock(ConsoleOutput)
        System.in = mock(InputStream)

        DefaultPrompter prompter = new DefaultPrompter(out)
        expectException(PrompterException) { prompter.prompt("hello") }
    }

    static void expectInput(String text) {
        System.in = new ByteArrayInputStream(text.bytes)
    }

    static Throwable expectException(Class<? extends Throwable> catchMe, Closure callMe) {
        try {
            callMe.call()
            Assert.fail("Expected ${catchMe.getName()} to be thrown.")
        } catch(e) {
            if (!e.class.isAssignableFrom(catchMe)) {
                throw e
            }
            return e
        }
    }

    static class StubPromptOption implements PromptOption<String> {

        private final name
        private final value

        StubPromptOption(name, value) {
            this.name = name
            this.value = value
        }

        @Override
        String displayName() {
            return name
        }

        @Override
        String value() {
            return this.value
        }
    }
}
