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
package com.okta.cli.common.progressbar;

import java.io.PrintStream;

class NonInteractiveProgressBar implements ProgressBar {

    private final PrintStream stream;

    public NonInteractiveProgressBar() {
        this(System.out);
    }

    NonInteractiveProgressBar(PrintStream stream) {
        this.stream = stream;
    }

    @Override
    public ProgressBar start() {
        return this;
    }

    @Override
    public ProgressBar start(CharSequence message) {
        info(message);
        return this;
    }

    @Override
    public void info(CharSequence message) {
        if (message != null) {
            stream.println(message);
        }
    }

    @Override
    public void close() {

    }
}
