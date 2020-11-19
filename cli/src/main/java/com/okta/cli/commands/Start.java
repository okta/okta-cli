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

import com.okta.cli.commands.apps.CommonAppsPrompts;
import com.okta.cli.common.config.MapPropertySource;
import com.okta.cli.common.config.MutablePropertySource;
import com.okta.cli.common.model.AuthorizationServer;
import com.okta.cli.common.model.FilterConfigBuilder;
import com.okta.cli.common.model.OktaSampleConfig;
import com.okta.cli.common.model.SamplesListings;
import com.okta.cli.common.service.ClientConfigurationException;
import com.okta.cli.common.service.DefaultInterpolator;
import com.okta.cli.common.service.DefaultSampleConfigParser;
import com.okta.cli.common.service.DefaultSdkConfigurationService;
import com.okta.cli.common.service.DefaultSetupService;
import com.okta.cli.common.service.DefaultStartRestClient;
import com.okta.cli.common.service.TarballExtractor;
import com.okta.cli.console.ConsoleOutput;
import com.okta.cli.console.PromptOption;
import com.okta.commons.lang.Assert;
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
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.okta.cli.common.service.SampleConfigParser.SAMPLE_CONFIG_PATH;

@CommandLine.Command(name = "start",
                     description = "Creates an Okta Sample Application")
public class Start extends BaseCommand {

    @CommandLine.Parameters(description = "Name of sample", arity = "0..1")
    private String sampleName;

    @CommandLine.Option(names = {"--branch", "-b"}, description = "GitHub branch to use", hidden = true, defaultValue = "master")
    private String branchName;

    @Override
    public int runCommand() throws Exception {

        // registration is required, walk through the registration flow if needed
        Register.requireRegistration(getStandardOptions());

        final String appName;
        final File projectDirectory;
        final boolean extractedProject;

        // sampleName defined, unzip tarball into a new directory
        if (!Strings.isEmpty(sampleName)) {
            appName = "okta-" + sampleName + "-sample";
            projectDirectory = new File(sampleName).getCanonicalFile();

            // TODO - make this constant or config
            String url = "https://github.com/okta-samples/" + appName + "/tarball/" + branchName;
            extractSample(url, projectDirectory);
            extractedProject = true;

        // check for existing .okta/.okta.yaml
        } else if (new File(SAMPLE_CONFIG_PATH).exists()) {
            projectDirectory = new File(".").getCanonicalFile();
            appName = projectDirectory.getName();
            extractedProject = false;

        // other, get the list of samples from start.okta.dev and let the user pick them
        } else {
            // get list of samples
            List<PromptOption<SamplesListings.OktaSample>> sampleOptions = new DefaultStartRestClient().listSamples().stream()
                    .map(sample -> PromptOption.of(sample.getDescription(), sample))
                    .collect(Collectors.toList());

            Assert.notEmpty(sampleOptions, "Failed to get the list of example applications. Check your network connection and try to rerun this command.");

            // prompt for selection
            SamplesListings.OktaSample sample = getPrompter().prompt("Select a sample", sampleOptions, sampleOptions.get(0));

            appName = "okta-" + sample.getName() + "-sample";
            projectDirectory = new File(appName).getCanonicalFile();
            // extract the selected sample
            extractSample(sample.getTarballUrl(), projectDirectory);
            extractedProject = true;
        }

        // TODO need to better abstract away the ~/.okta/okta.yaml config values
        Map<String, String> sampleContext = new FilterConfigBuilder().setOrgUrl(oktaBaseUrl()).build();

        // parse the `.okta.yaml` file
        OktaSampleConfig config = new DefaultSampleConfigParser().loadConfig(projectDirectory, sampleContext);
        OpenIdConnectApplicationType applicationType = OpenIdConnectApplicationType.valueOf(
                config.getOAuthClient().getApplicationType().toUpperCase(Locale.ENGLISH));

        // create the Okta application
        Client client = Clients.builder().build();
        AuthorizationServer authorizationServer = CommonAppsPrompts.getIssuer(client, getPrompter(), null);
        MutablePropertySource propertySource = new MapPropertySource();
        new DefaultSetupService(null).createOidcApplication(
                propertySource,
                appName,
                null,
                null,
                Collections.emptySet(),
                authorizationServer.getIssuer(),
                authorizationServer.getId(),
                true,
                applicationType,
                config.getOAuthClient().getRedirectUris(),
                config.getOAuthClient().getPostLogoutRedirectUris(),
                config.getTrustedOrigins()
        );

        // filter config file
        Map<String, String> context = new FilterConfigBuilder()
                .fromPropertySource(propertySource)
                .setIssuerId(authorizationServer.getId())
                .build();

        // walk directory structure, ignore .okta
        Files.walkFileTree(projectDirectory.toPath(), new SampleFileVisitor(context));

        ConsoleOutput out = getConsoleOutput();

        // provide instructions to user
        if (!Strings.isEmpty(config.getDirections())) {

            // Tell the user to cd into the new dir if needed
            if (extractedProject) {
                out.writeLine("Change the directory:");
                out.writeLine("    cd " + projectDirectory.getName() + "\n");
            }
            out.writeLine(config.getDirections());
        }

        return 0;
    }

    private void extractSample(String url, File projectDirectory) {
        try {
            // extract the remote zip
            new TarballExtractor().extract(url, projectDirectory);
        } catch (IOException e) {
            throw new CliFailureException("Failed to extract tarball from URL: " + url, e);
        }
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

    private String oktaBaseUrl() throws ClientConfigurationException {
        return new DefaultSdkConfigurationService().loadUnvalidatedConfiguration().getBaseUrl();
    }
}
