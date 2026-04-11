package com.baseapp.infrastructure.adapter;

import com.baseapp.domain.model.HelloMessage;
import com.baseapp.domain.port.HelloPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HelloAdapter implements HelloPort {

    @Override
    public HelloMessage buildMessage() {
        return new HelloMessage("Hello from k3s 🚀", Instant.now());
    }
}
