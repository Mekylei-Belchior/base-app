package com.baseapp.entrypoint.rest;

import com.baseapp.application.usecase.HelloUseCase;
import com.baseapp.domain.model.HelloMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HelloUseCase helloUseCase;

    @Test
    void getHello_shouldReturn200WithJson() throws Exception {
        Instant now = Instant.parse("2026-04-09T12:00:00Z");
        when(helloUseCase.execute()).thenReturn(new HelloMessage("Hello from k3s 🚀", now));

        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Hello from k3s 🚀"))
                .andExpect(jsonPath("$.timestamp").value("2026-04-09T12:00:00Z"));
    }

    @Test
    void getHello_shouldReturnMessageAndTimestampFields() throws Exception {
        when(helloUseCase.execute()).thenReturn(new HelloMessage("Hi", Instant.now()));

        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
