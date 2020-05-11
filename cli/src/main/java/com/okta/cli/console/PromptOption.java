package com.okta.cli.console;

public interface PromptOption<T> {

    String displayName();

    T value();

    static <T> PromptOption<T> of(String name, T value) {
        return new PromptOption<>() {
            @Override
            public String displayName() {
                return name;
            }

            @Override
            public T value() {
                return value;
            }
        };
    }
}
