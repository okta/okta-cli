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

import com.okta.commons.lang.Classes
import org.hamcrest.*

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration
import java.util.concurrent.*
import java.util.stream.Collectors

class CommandRunner {

    private final String regServiceUrl

    private final File homeDir = File.createTempDir()
    private final File workingDir = new File(File.createTempDir(), "test-project")

    private Closure initHomeDir;

    CommandRunner() {
        this(null)
    }

    CommandRunner(String regServiceUrl) {
        this.regServiceUrl = regServiceUrl

        this.workingDir.mkdirs()
    }

    CommandRunner withHomeDirectory(Closure initHomeDir) {
        this.initHomeDir = initHomeDir
        setupHomeDir(homeDir)
        return this
    }

    CommandRunner withSdkConfig(String baseUrl, String token="some-test-token") {
        this.withHomeDirectory( {
            File file = new File(homeDir,".okta/okta.yaml")
            file.getParentFile().mkdir()
            file.write "okta:\n"
            file << "  client:\n"
            file << "    orgUrl: ${baseUrl}\n"
            file << "    token: ${token}\n"
        })
        return this
    }

    Result runCommand(String... args) {
        runCommandWithInput(null, args)
    }

    Result runCommandWithInput(List<String> input, String... args) {

        return (isIde()) // if intellij
            ? runInIsolatedClassloader([] as String[], args, input)
            : runProcess([] as String[], args, input)
    }

    // TODO: this only works some time, consider removing this
    static boolean isIde() {
        return false && System.getProperty("java.class.path").contains("idea_rt.jar") && Classes.isAvailable("com.okta.cli.OktaCli")
    }

    Result runProcess(String[] envVars, String[] args, List<String> input) {

        String homeDirString = escapePath(homeDir.absolutePath)

        List<String> command = [getCli(homeDir), "-Duser.home=${homeDirString}", "-Dokta.testing.disableHttpsCheck=true", "-Dokta.cli.registrationUrl=${regServiceUrl}", "-Dokta.cli.apiUrl=${regServiceUrl}"]
        command.addAll(args)

        String cmd = command.join(" ")

        def sout = new StringBuilder()
        def serr = new StringBuilder()
        def process = Runtime.getRuntime().exec(cmd, null, workingDir)

        // We are seeing some issues with Groovy's process.consumeProcessOutput(sout, serr) method
        // It looks like there are some threading issues the show up in CI with Linux VMs. Likely what is happening
        // is the threads used to consume the sout and serr finish at some point after process.waitForOrKill finishes
        // any remaining output is lost
        def soutThread = process.consumeProcessOutputStream(sout)
        def serrThread = process.consumeProcessErrorStream(serr)

        Thread.sleep(100)

        if (process.isAlive() && input != null && !input.empty) {
            def writer = new OutputStreamWriter(process.getOutputStream())
            input.forEach {
                writer.write(it)
                writer.write("\n")
                writer.flush()
            }
        }

        // Wait for the process to finish
        try {
            process.waitForOrKill(timeout().toMillis())
            // wait for the output consumption threads to finish, these will throw if they don't finish in 100ms
            soutThread.join(100)
            serrThread.join(100)
        } finally {
            process.closeStreams()
        }

        return new Result(process.exitValue(), cmd, envVars, sout.toString(), serr.toString(), workingDir, homeDir)
    }

    Result runInIsolatedClassloader(String[] envVars, String[] args, List<String> input) {

        RestoreEnvironmentVariables restoreEnvironmentVariables = new RestoreEnvironmentVariables()
        restoreEnvironmentVariables.saveValues()
        RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties()
        restoreSystemProperties.saveValues()

        envVars.each {
            String[] parts = it.split("=")
            RestoreEnvironmentVariables.setEnvironmentVariable(parts[0], parts[1])
        }

        System.setProperty("user.home", homeDir.absolutePath)

        PrintStream originalOut = System.out
        PrintStream originalErr = System.err
        InputStream originalIn = System.in
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        ByteArrayOutputStream err = new ByteArrayOutputStream()
        ByteArrayInputStream testInput = new ByteArrayInputStream(input.join("\n").getBytes(StandardCharsets.UTF_8))

        ExecutorService executorService = Executors.newFixedThreadPool(1)

        int exitCode = -255

        try {
            System.out = new PrintStream(out)
            System.err = new PrintStream(err)
            System.in = testInput

            Callable<Integer> callable = new Callable<Integer>() {
                @Override
                Integer call() throws Exception {
                    // isolate the classpath so the static lookups of the User Home dir are reload
                    URL[] classPath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                        .map { new File(it).toURI().toURL() }
                        .collect(Collectors.toList())
                    Thread.currentThread().setContextClassLoader(new URLClassLoader(classPath))

                    return com.okta.cli.OktaCli.run(args)
                }
            }

            Future<Integer> future = executorService.submit(callable)
            exitCode = future.get(timeout().toSeconds(), TimeUnit.SECONDS)
            executorService.shutdown()

        } catch(TimeoutException e) {
            e.printStackTrace(originalErr)
        } finally {
            System.out = originalOut
            System.err = originalErr
            System.in = originalIn
            restoreEnvironmentVariables.restoreOriginalVariables()
            restoreSystemProperties.restoreOriginalVariables()
        }

        return new Result(exitCode, "OktaCli.run(${args})", envVars, out.toString(), err.toString(), workingDir, homeDir)
    }

    protected void setupHomeDir(File homeDir) {
        if (initHomeDir != null) {
            initHomeDir.call(homeDir)
        }
    }

    static String getCli(File homeDir) {
//        String javaExec = new File(System.getProperty("java.home"), "bin/java").absolutePath
//        String jarFile = new File("../cli/target/okta-cli-0.7.1-SNAPSHOT.jar").absolutePath
//        String cli = "${javaExec} -Duser.home=##user.home## -jar ${jarFile}"

        String cli = System.getProperty("okta-cli-test.path")
        if (cli == null || cli.isBlank()) {
            File defaultExec = isWindows() ?
                    new File("../cli/target/okta.exe") :
                    new File("../cli/target/okta")
            defaultExec = defaultExec.absoluteFile.canonicalFile
            return escapePath(defaultExec.absolutePath)
        }

        // setting the home directory is tricky, so fitler it into the command
        return cli.replace("##user.home##", escapePath(homeDir.absolutePath))
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows")
    }

    private static String escapePath(String file) {
        return ifWindows(file, { return it.replace("\\", "\\\\") })
    }

    static <T> T ifWindows(T originalValue, Closure<T> closure) {
        if (isWindows()) {
            return closure.call(originalValue)
        } else {
            return originalValue
        }
    }

    static File jarFile() {

        Path startDir = Paths.get("../cli/target/")
        String pattern = "okta-cli-*.jar"

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern)
        List<File> matches = new ArrayList<>()

        FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
                if (matcher.matches(file.getFileName())) {
                    matches.add(file.toFile())
                }
                return FileVisitResult.CONTINUE
            }
        };
        Files.walkFileTree(startDir, matcherVisitor)
        MatcherAssert.assertThat(matches, Matchers.hasSize(1))

        return matches.get(0)
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

    private static Duration timeout() {
        String timeout = System.getenv("OKTA_ITS_TIMEOUT")
        return timeout != null ?
                Duration.parse(timeout) :
                Duration.ofSeconds(30)

    }
}