package com.okta.cli.test

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher

class CommandRunner {

    private final String regServiceUrl

    CommandRunner() {
        this(null)
    }

    CommandRunner(String regServiceUrl) {
        this.regServiceUrl = regServiceUrl
    }

    Result runCommand(String... args) {
        runCommandWithInput(null, args)
    }

    Result runCommandWithInput(List<String> input, String... args) {

        File homeDir = File.createTempDir()
        File workingDir = new File(File.createTempDir(), "test-project")
        workingDir.mkdirs()

        String command = [cli, "-Duser.home=${homeDir}", args].flatten().join(" ")
        String[] envVars = ["OKTA_CLI_BASE_URL=${regServiceUrl}"]

        def process = Runtime.getRuntime().exec(command, envVars, workingDir)
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        process.consumeProcessOutput(sout, serr)

        if (process.isAlive() && input != null && !input.empty) {
            def writer = new OutputStreamWriter(process.getOutputStream())
            input.forEach {
                writer.write(it)
                writer.write("\n")
                writer.flush()
            }
        }

        process.waitForOrKill(10000)

        return new Result(process.exitValue(), command, envVars, sout.toString(), serr.toString(), workingDir, homeDir)
    }

    static String getCli() {
        String cli = System.getProperty("okta-cli-test.path")
        if (cli == null || cli.isBlank()) {
            return new File("../cli/target/okta-cli").absolutePath
        }
        return cli
    }

    static class Result {
        final int exitCode
        final String command;
        final String[] envVars
        final String stdOut
        final String stdErr
        final File workDir
        final File homeDir

        Result(int exitCode, String command, String[] envVars, String stdOut, String stdErr, File workDir, File homeDir) {
            this.exitCode = exitCode
            this.command = command
            this.envVars = envVars
            this.stdOut = stdOut
            this.stdErr = stdErr
            this.workDir = workDir
            this.homeDir = homeDir
        }
    }

    static ResultMatcher resultMatches(Integer exitCode, Matcher<String> standardOutMatcher, Matcher<String> standardErrMatcher) {
        return resultMatches(Matchers.equalTo(exitCode), standardOutMatcher, standardErrMatcher)
    }

    static ResultMatcher resultMatches(Matcher<Integer> exitCodeMatcher, Matcher<String> standardOutMatcher, Matcher<String> standardErrMatcher) {
        return new ResultMatcher(exitCodeMatcher, standardOutMatcher, standardErrMatcher)
    }

    static class ResultMatcher extends TypeSafeMatcher<Result> {

        private final Matcher<Integer> exitCodeMatcher
        private final Matcher<String> standardOutMatcher
        private final Matcher<String> standardErrMatcher

        ResultMatcher(Matcher<Integer> exitCodeMatcher, Matcher<String> standardOutMatcher, Matcher<String> standardErrMatcher) {
            this.exitCodeMatcher = exitCodeMatcher
            this.standardOutMatcher = standardOutMatcher
            this.standardErrMatcher = standardErrMatcher
        }

        @Override
        protected boolean matchesSafely(Result result) {

            boolean matches = result != null
            if (matches && exitCodeMatcher != null) {
                matches &= exitCodeMatcher.matches(result.exitCode)
            }

            if (matches && standardOutMatcher != null) {
                matches &= standardOutMatcher.matches(result.stdOut)
            }

            if (matches && standardErrMatcher != null) {
                matches &= standardErrMatcher.matches(result.stdErr)
            }
            return matches
        }

        @Override
        void describeTo(Description description) {
            description.appendText("A Result with:")
            if (exitCodeMatcher != null) {
                description.appendText("\n\t\tExit Status: ")
                description.appendDescriptionOf(exitCodeMatcher)
            }

            if (standardOutMatcher != null) {
                description.appendText("\n\t\tStandard Out: ")
                description.appendDescriptionOf(standardOutMatcher)
            }

            if (standardErrMatcher != null) {
                description.appendText("\n\t\tStandard Err: ")
                description.appendDescriptionOf(standardErrMatcher)
            }
        }

        @Override
        protected void describeMismatchSafely(Result item, Description mismatchDescription) {
            mismatchDescription.appendText("A Result with:")

            mismatchDescription.appendText("\n\t\tExit Status: ")
            mismatchDescription.appendValue(item.exitCode)

            mismatchDescription.appendText("\n\t\tStandard Out: ")
            mismatchDescription.appendText("\"")
            mismatchDescription.appendText(item.stdOut)
            mismatchDescription.appendText("\"")

            mismatchDescription.appendText("\n\t\tStandard Err: ")
            mismatchDescription.appendText("\"")
            mismatchDescription.appendText(item.stdErr)
            mismatchDescription.appendText("\"")
        }
    }
}