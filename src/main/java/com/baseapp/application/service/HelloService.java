package com.baseapp.application.service;

import com.baseapp.application.usecase.HelloUseCase;
import com.baseapp.domain.model.HelloMessage;
import com.baseapp.domain.port.HelloPort;
import org.springframework.stereotype.Service;

@Service
public class HelloService implements HelloUseCase {

    private final HelloPort helloPort;

    public HelloService(HelloPort helloPort) {
        this.helloPort = helloPort;
    }

    @Override
    public HelloMessage execute() {
        return helloPort.buildMessage();
    }
}
