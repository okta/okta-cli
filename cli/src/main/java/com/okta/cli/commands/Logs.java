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
import com.okta.cli.console.ConsoleOutput;
import com.okta.commons.lang.Strings;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.impl.resource.AbstractCollectionResource;
import com.okta.sdk.resource.log.LogEvent;
import com.okta.sdk.resource.log.LogEventList;
import com.okta.sdk.resource.log.LogOutcome;
import com.okta.sdk.resource.log.LogSeverity;
import picocli.CommandLine;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

import static java.lang.Thread.sleep;

@CommandLine.Command(name = "logs",
                     description = "Lists Okta log events",
                     hidden = true)
public class Logs implements Callable<Integer> {

    @CommandLine.Mixin
    private OktaCli.StandardOptions standardOptions;

    @CommandLine.Option(names = {"-f", "--follow"}, description = "Polls for new log events")
    protected boolean follow;

    @Override
    public Integer call() throws Exception {

        Client client = Clients.builder().build();
        ConsoleOutput output = standardOptions.getEnvironment().getConsoleOutput();

        // At most 1 hour back
        String since = Instant.now().minus(1, ChronoUnit.HOURS).toString();
        LogEventList logs = client.getLogs(null, since, null, null, null);

        output.bold("Time                      Severity  Status     Message\n");
        logs.stream().forEach(log -> writeEvent(output, logsBaseUrl(logs), log));

        if (follow) {
            String pagingUrl = nextUrlPage(null, logs);

            while (true) {
                sleep(2000);
                //"published gt \"" + lastEvent + "\""
                LogEventList moreLogs = client.http().get(pagingUrl, LogEventList.class);
                moreLogs.stream()
                        .forEach(log -> writeEvent(output, logsBaseUrl(moreLogs), log));
                pagingUrl = nextUrlPage(pagingUrl, moreLogs);
            }
        }

        return 0;
    }

    private String logsBaseUrl(LogEventList logs) {
        String url = logs.getResourceHref();
        if (url.startsWith("/")) {
            url = nextUrlPage(null, logs);
        }
        return url.replaceFirst("/api/v1/logs.*", "/api/v1/logs");
    }

    private String nextUrlPage(String currentPage, LogEventList logs) {
        // TODO dependency on the SDK `impl` module, the SDK will automatically follow result until it
        // finds an empty page for log polling we want to retry the empty page (after a short delay)
        if (!(logs instanceof AbstractCollectionResource)) {
            throw new IllegalStateException("LogEventList must be an instance of AbstractCollectionResource to resolve.");
        }
        String pagingUrl = ((AbstractCollectionResource) logs).getString("nextPage");
        return pagingUrl != null
                ? pagingUrl
                : currentPage;
    }

    private void writeEvent(ConsoleOutput output, String logUrl, LogEvent log) {
        output.write(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.SSSX").withZone(ZoneOffset.UTC).format(log.getPublished().toInstant()));
        output.write("  ");
        writeSeverity(output, log.getSeverity());
        output.write("      ");
        writeStatus(output, log.getOutcome());
        output.write("  ");
        output.write(trim(log.getDisplayMessage()));
        output.write("  ");
        output.write(logUrl + "?filter=uuid+eq+%22" + log.getUuid() + "%22");
        output.writeLine("");
    }

    private static void writeStatus(ConsoleOutput output, LogOutcome outcome) {

        String format = "%-9s";
        if (outcome == null) {
            output.write(String.format(format, ""));
        } else {
            String result = outcome.getResult();
            switch (result) {
                case "SKIPPED":
                    output.writeWarning(String.format(format, result));
                    break;
                case "FAILURE":
                case "DENY":
                    output.writeError(String.format(format, result));
                    break;
                default:
                    output.write(String.format(format, result));
            }
        }
    }

    private static void writeSeverity(ConsoleOutput output, LogSeverity severity) {
        switch (severity) {
            case WARN:
                output.writeWarning(severity);
                break;
            case ERROR:
                output.writeError(severity);
                break;
            default:
                output.write(severity);
        }
    }

    private static String trim(String input) {
        return input == null
                ? ""
                : Strings.trimWhitespace(input);
    }
}