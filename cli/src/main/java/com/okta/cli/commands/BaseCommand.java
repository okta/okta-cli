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
package com.okta.cli.commands;

import com.okta.cli.Environment;
import com.okta.cli.OktaCli;
import com.okta.cli.common.model.Semver;
import com.okta.cli.common.model.VersionInfo;
import com.okta.cli.common.service.DefaultStartRestClient;
import com.okta.cli.common.service.StartRestClient;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.Prompter;
import picocli.CommandLine;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class BaseCommand implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    private final StartRestClient restClient;

    public BaseCommand() {
        this(new DefaultStartRestClient());
    }

    BaseCommand(StartRestClient restClient) {
        this.restClient = restClient;
    }

    public BaseCommand(OktaCli.StandardOptions standardOptions) {
        this();
        this.standardOptions = standardOptions;
    }

    BaseCommand(StartRestClient restClient, OktaCli.StandardOptions standardOptions) {
        this(restClient);
        this.standardOptions = standardOptions;
    }

    protected abstract int runCommand() throws Exception;

    @Override
    public Integer call() throws Exception {

        // Before running the command, kick off a thread to get the latest version
        Semver currentVersion = getCurrentVersion();
        Future<Optional<VersionInfo>> future = asyncVersionInfo(currentVersion);

        // run the actual command
        int exitCode = runCommand();

        // After the command finishes alert the user if needed
        handleVersionInfo(future, currentVersion);
        return exitCode;
    }

    protected OktaCli.StandardOptions getStandardOptions() {
        return standardOptions;
    }

    protected ConsoleOutput getConsoleOutput() {
        return standardOptions.getEnvironment().getConsoleOutput();
    }

    protected Prompter getPrompter() {
        return standardOptions.getEnvironment().prompter();
    }

    protected Environment getEnvironment() {
        return standardOptions.getEnvironment();
    }

    private Future<Optional<VersionInfo>> asyncVersionInfo(Semver currentVersion) {

        Callable<Optional<VersionInfo>> versionInfoCallable = () -> {
            // if the shell is NOT interactive skip the version check, it's being used in a script
            if (currentVersion.isReleaseBuild() && standardOptions.getEnvironment().isInteractive()) {
                return Optional.of(restClient.getVersionInfo());
            } else {
                return Optional.empty();
            }
        };
        return Executors.newSingleThreadExecutor().submit(versionInfoCallable);
    }

    // protected to allow for testing
    Semver getCurrentVersion() {
        return Semver.parse(OktaCli.VERSION);
    }

    private void handleVersionInfo(Future<Optional<VersionInfo>> future, Semver currentVersion) {

        try {
            future.get(2, TimeUnit.SECONDS)
                .ifPresent(info -> {
                    if (Semver.parse(info.getLatestVersion()).isGreaterThan(currentVersion)) {
                        ConsoleOutput out = getConsoleOutput();
                        out.writeLine("");
                        out.bold("A new version of the Okta CLI is available: " + info.getLatestVersion());
                        out.writeLine("");
                        getConsoleOutput().bold("See what's new: " + info.getLatestReleaseUrl());
                        out.writeLine("");
                    }
                });
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // by default do NOT show any errors from fetching the version
            if (standardOptions.isVerbose()) {
                getConsoleOutput().writeError("Failed to fetch latest CLI Version:");
                e.printStackTrace();
            }
        }
    }
}
