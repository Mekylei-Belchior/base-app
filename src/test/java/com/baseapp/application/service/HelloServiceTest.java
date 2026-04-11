package com.baseapp.application.service;

import com.baseapp.domain.model.HelloMessage;
import com.baseapp.domain.port.HelloPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelloServiceTest {

    @Mock
    private HelloPort helloPort;

    private HelloService helloService;

    @BeforeEach
    void setUp() {
        helloService = new HelloService(helloPort);
    }

    @Test
    void execute_shouldReturnMessageFromPort() {
        HelloMessage expected = new HelloMessage("Hello from k3s 🚀", Instant.now());
        when(helloPort.buildMessage()).thenReturn(expected);

        HelloMessage result = helloService.execute();

        assertThat(result).isEqualTo(expected);
        verify(helloPort, times(1)).buildMessage();
    }

    @Test
    void execute_shouldDelegateToPortExactlyOnce() {
        when(helloPort.buildMessage()).thenReturn(new HelloMessage("test", Instant.now()));

        helloService.execute();

        verify(helloPort, times(1)).buildMessage();
    }
}
