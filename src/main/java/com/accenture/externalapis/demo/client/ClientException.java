package com.accenture.externalapis.demo.client;

public class ClientException extends RuntimeException {

    private final boolean retryable;

    public ClientException(String message) {
        this(message, null, false);
    }

    public ClientException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public ClientException(String message, boolean retryable) {
        this(message, null, retryable);
    }

    public ClientException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
