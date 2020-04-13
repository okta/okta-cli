package com.okta.cli.prompter;

public class PrompterException extends RuntimeException {

    public PrompterException(String message) {
        super(message);
    }

    public PrompterException(String message, Throwable cause) {
        super(message, cause);
    }
}
