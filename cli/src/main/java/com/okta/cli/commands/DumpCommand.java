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
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "dump", description = "Dump environment for debugging", hidden = true)
public class DumpCommand implements Callable<Integer> {

    @CommandLine.Mixin
    OktaCli.StandardOptions standardOptions;
    
    @Override
    public Integer call() throws Exception {

        ConsoleOutput out = standardOptions.getEnvironment().getConsoleOutput();

        out.writeLine("Dumping environment");
        System.getenv().forEach((key, value) -> {
            out.write("\t");
            out.write(key);
            out.write(" = ");
            out.writeLine(value);
        });

        out.writeLine("System Properties");
        System.getProperties().forEach((key, value) -> {
            out.write("\t");
            out.write(key);
            out.write(" = ");
            out.writeLine(value);
        });

        return 0;
    }
}
