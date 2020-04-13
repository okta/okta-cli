package com.okta.cli.common.service;

public class ClientConfigurationException extends Exception {

    public ClientConfigurationException(String message) {
        super(message);
    }

    public ClientConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
