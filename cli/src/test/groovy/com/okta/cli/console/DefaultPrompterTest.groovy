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

import com.okta.cli.TimeoutListener
import org.testng.Assert
import org.testng.annotations.Listeners
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.nullValue
import static org.mockito.Mockito.*

@Listeners([RestoreSystemInOut, TimeoutListener])
class DefaultPrompterTest {

    @Test
    void basicPrompter() {

        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("test-result")
        DefaultPrompter prompter = new DefaultPrompter(out)
        String result = prompter.prompt("hello")

        assertThat(result, equalTo("test-result"))
        verify(out).write("hello: ")
    }

    @Test
    void nullResult() {

        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("")
        DefaultPrompter prompter = new DefaultPrompter(out)
        String result = prompter.prompt("hello")

        assertThat(result, nullValue())
    }

    @Test
    void trimInput() {
        ConsoleOutput out = mock(ConsoleOutput)

        expectInput(" test-result \t ")
        DefaultPrompter prompter = new DefaultPrompter(out)
        String result = prompter.prompt("hello")

        assertThat(result, equalTo("test-result"))
        verify(out).write("hello: ")
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
    void failToReadLine() {
        ConsoleOutput out = mock(ConsoleOutput)
        System.in = mock(InputStream)

        DefaultPrompter prompter = new DefaultPrompter(out)
        expectException(PrompterException) { prompter.prompt("hello") }
    }

    @Test
    void promptYesNo_defaultYes() {
        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("")
        DefaultPrompter prompter = new DefaultPrompter(out)
        boolean result = prompter.promptYesNo("Are you good?")

        assertThat("expected prompter default to 'true'", result)
        verify(out).write("Are you good? [")
        verify(out).bold("Y")
        verify(out).write("/n")
        verify(out).write("]")
    }

    @Test
    void promptYesNo_defaultNo() {
        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("")
        DefaultPrompter prompter = new DefaultPrompter(out)
        boolean result = prompter.promptYesNo("Are you good?", false)

        assertThat("expected prompter default to 'false'", !result)
        verify(out).write("Are you good? [")
        verify(out).write("y/")
        verify(out).bold("N")
        verify(out).write("]")
    }

    @Test
    void promptYesNo_no() {
        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("no")
        DefaultPrompter prompter = new DefaultPrompter(out)
        boolean result = prompter.promptYesNo("Are you good?", true)

        assertThat("expected prompter default to 'false'", !result)
        verify(out).write("Are you good? [")
        verify(out).bold("Y")
        verify(out).write("/n")
        verify(out).write("]")
    }

    @Test
    void promptYesNo_Other() {
        ConsoleOutput out = mock(ConsoleOutput)

        expectInput("foobar\nyes")
        DefaultPrompter prompter = new DefaultPrompter(out)
        boolean result = prompter.promptYesNo("Are you good?", true)

        assertThat("expected prompter default to 'true'", result)
        verify(out, times(2)).write("Are you good? [")
        verify(out, times(2)).bold("Y")
        verify(out, times(2)).write("/n")
        verify(out, times(2)).write("]")
        verify(out, times(1)).writeError("\nInvalid choice, try again\n\n")
    }

    @Test
    void pauseEnter() {
        ConsoleOutput out = mock(ConsoleOutput)
        expectInput("\n")
        DefaultPrompter prompter = new DefaultPrompter(out)
        prompter.pause()
        verify(out).writeLine("(Press Enter to continue)")
    }

    @Test
    void pauseWithExtraInput() {
        ConsoleOutput out = mock(ConsoleOutput)
        expectInput("  something   was typed before  pressing... Enter\n")
        DefaultPrompter prompter = new DefaultPrompter(out)
        prompter.pause()
        verify(out).writeLine("(Press Enter to continue)")
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
