package com.baseapp.domain.model;

import java.time.Instant;

public class HelloMessage {

    private final String message;
    private final Instant timestamp;

    public HelloMessage(String message, Instant timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
