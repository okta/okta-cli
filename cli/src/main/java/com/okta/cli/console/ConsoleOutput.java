package com.okta.cli.console;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;

public interface ConsoleOutput extends Closeable {

    void write(String msg);

    default void write(Object obj) {
        write(obj + "");
    }

    void writeLine(String msg);

    default void writeLine(Object obj) {
        writeLine(obj + "");
    }

    void flush();

    public static ConsoleOutput create(boolean colors) {

        return new AnsiConsoleOutput(System.out, colors && !System.getProperty("os.name").startsWith("Windows"));
    }

    class AnsiConsoleOutput implements ConsoleOutput {

        private final PrintStream out;

        private final boolean colors;

        public AnsiConsoleOutput(PrintStream out, boolean colors) {
            this.out = out;
            this.colors = colors;
        }

        @Override
        public void write(String msg) {
            out.print(msg);
        }

        @Override
        public void writeLine(String msg) {
            out.println(msg);
            flush();
        }

        @Override
        public void flush() {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }
}
