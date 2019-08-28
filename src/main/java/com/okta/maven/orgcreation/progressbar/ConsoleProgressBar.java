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
package com.okta.maven.orgcreation.progressbar;

import java.io.PrintStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

class ConsoleProgressBar implements ProgressBar {

    private final ProgressBarWriter progressBarWriter;
    private final PrintStream stream;
    private final Thread thread;

    ConsoleProgressBar() {
        this(System.out, Duration.ofMillis(500)); // half second
    }

    ConsoleProgressBar(PrintStream stream, Duration updateInterval) {
        this.stream = stream;
        this.progressBarWriter = new ProgressBarWriter(stream, updateInterval);
        this.thread = new Thread(progressBarWriter, ConsoleProgressBar.class.getName());
    }

    @Override
    public ProgressBar start() {
        this.thread.start();
        return this;
    }

    @Override
    public ProgressBar start(CharSequence message) {
        stream.println(message);
        return start();
    }

    @Override
    public void info(CharSequence message) {
        stream.println("\r" + message);
    }

    @Override
    public void close() {
        progressBarWriter.stop();

        thread.interrupt();
        try {
            thread.join();
        }
        catch (InterruptedException ignored) { }
    }

    static class ProgressBarWriter implements Runnable {

        private final PrintStream stream;
        private final Duration updateInterval;

        private final List<Character> animationChars = Arrays.asList('/', '-', '\\', '|');

        private boolean running = true;

        ProgressBarWriter(PrintStream stream, Duration updateInterval) {
            this.stream = stream;
            this.updateInterval = updateInterval;
        }

        @Override
        public void run() {

            int ii = 0;
            do {
                stream.print('\r');

                char nextChar = animationChars.get(ii++ % animationChars.size());
                stream.print(nextChar);

                try {
                    Thread.sleep(updateInterval.toMillis());
                } catch (InterruptedException e) {
                    finish();
                }

            } while (running && !Thread.interrupted());
        }

        void stop() {
            running = false;
        }

        void finish() {
            stream.println("\r");
            stream.flush();
        }
    }
}
