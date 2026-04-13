package com.baseapp.application.service;

import com.baseapp.domain.model.HelloMessage;
import com.baseapp.domain.port.out.HelloMessageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelloServiceTest {

    @Mock
    private HelloMessageProvider messageProvider;

    private HelloService helloService;

    @BeforeEach
    void setUp() {
        helloService = new HelloService(messageProvider);
    }

    @Test
    void execute_shouldReturnMessageWithTextFromProvider() {
        when(messageProvider.provideMessageText()).thenReturn("Hello from k3s \uD83D\uDE80");

        HelloMessage result = helloService.execute();

        // Record accessor: result.message() em vez de result.getMessage()
        assertThat(result.message()).isEqualTo("Hello from k3s \uD83D\uDE80");
        assertThat(result.timestamp()).isNotNull();
        verify(messageProvider, times(1)).provideMessageText();
    }

    @Test
    void execute_shouldDelegateToProviderExactlyOnce() {
        when(messageProvider.provideMessageText()).thenReturn("test");

        helloService.execute();

        verify(messageProvider, times(1)).provideMessageText();
    }
}
