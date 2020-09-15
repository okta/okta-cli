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

import com.okta.cli.OktaCli;
import com.okta.cli.commands.apps.CommonAppsPrompts;
import com.okta.cli.common.config.MapPropertySource;
import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.AuthorizationServer;
import com.okta.cli.common.model.FilterConfigBuilder;
import com.okta.cli.common.model.OktaSampleConfig;
import com.okta.cli.common.service.DefaultInterpolator;
import com.okta.cli.common.service.DefaultSampleConfigParser;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.TarballExtractor;
import com.okta.cli.console.ConsoleOutput;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.application.OpenIdConnectApplicationType;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.okta.cli.common.service.SampleConfigParser.SAMPLE_CONFIG_PATH;

@CommandLine.Command(name = "start",
                     description = "Creates an Okta Sample Application")
public class Start implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @CommandLine.Parameters(description = "Name of sample", arity = "0..1")
    private String sampleName;

    @CommandLine.Option(names = {"--branch", "-b"}, description = "GitHub branch to use", hidden = true, defaultValue = "wip")
    private String branchName;

    @Override
    public Integer call() throws Exception {

        // registration is required, walk through the registration flow if needed
        Register.requireRegistration(standardOptions);

        String localPath = "." + File.separator + sampleName;
        File projectDirectory = new File(localPath).getCanonicalFile();

        String appName;
        // sampleName defined, unzip tarball
        if (!Strings.isEmpty(sampleName)) {
            appName = "okta-" + sampleName + "-sample";
            // TODO - make this constant or config
            String url = "https://github.com/okta-samples/" + appName + "/tarball/" + branchName;
            try {
                // extract the remote zip
                new TarballExtractor().extract(url, projectDirectory);
            } catch (IOException e) {
                throw new CliFailureException("Failed to extract tarball from URL: " + url, e);
            }
        } else if (new File(localPath + File.separator + SAMPLE_CONFIG_PATH).exists()) {
            appName = projectDirectory.getName();

        // TODO default operation?
        } else {
            throw new CliFailureException("Running `okta start` must be run with a `sampleName` or run from a directory that contains a `.okta.yaml` file.");
        }

        // parse the `.okta.yaml` file
        OktaSampleConfig config = new DefaultSampleConfigParser().loadConfig(localPath);

        // create the Okta application
        Client client = Clients.builder().build();
        AuthorizationServer authorizationServer = CommonAppsPrompts.getIssuer(client, standardOptions.getEnvironment().prompter(), null);
        MutablePropertySource propertySource = new MapPropertySource();
        new DefaultSetupService(null).createOidcApplication(
                propertySource,
                appName,
                null,
                null,
                authorizationServer.getIssuer(),
                authorizationServer.getId(),
                true,
                OpenIdConnectApplicationType.valueOf(config.getOAuthClient().getApplicationType().toUpperCase(Locale.ENGLISH)), // TODO default to SPA
                config.getOAuthClient().getRedirectUris().toArray(new String[0])
        );

        // filter config file
        Map<String, String> context = new FilterConfigBuilder()
                .fromPropertySource(propertySource)
                .build();

        // walk directory structure, ignore .okta
        Files.walkFileTree(Paths.get(localPath), new SampleFileVisitor(context));

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();

        // provide instructions to user
        if (!Strings.isEmpty(config.getDirections())) {
            out.writeLine(config.getDirections());
        }

        return 0;
    }

    static class SampleFileVisitor extends SimpleFileVisitor<Path> {

        private Map<String, String> context;

        public SampleFileVisitor(Map<String, String> context) {
            this.context = context;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
            if (!attr.isRegularFile()) {
                return FileVisitResult.CONTINUE;
            }

            new DefaultInterpolator().interpolate(path, context);

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
            // TODO - make this a constant or project var
            if (".okta".equals(dir.getFileName().toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
