package com.baseapp.entrypoint.rest.dto;

import com.baseapp.domain.model.HelloMessage;

public record HelloResponse(String message, String timestamp) {

    public static HelloResponse from(HelloMessage domain) {
        return new HelloResponse(
                domain.getMessage(),
                domain.getTimestamp().toString()
        );
    }
}
