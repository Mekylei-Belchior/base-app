package com.baseapp.infrastructure.adapter.in.rest;

import com.baseapp.domain.port.in.HelloUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o GlobalExceptionHandler ativando os dois handlers via HelloController.
 *
 * @WebMvcTest carrega automaticamente todos os @RestControllerAdvice no contexto,
 * então não precisamos instanciar o handler diretamente.
 * Basta fazer o use case lançar a exceção desejada e verificar a resposta HTTP.
 */
@WebMvcTest(HelloController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HelloUseCase helloUseCase;

    @Test
    void handleIllegalArgument_shouldReturn400_whenIllegalArgumentExceptionIsThrown() throws Exception {
        when(helloUseCase.execute()).thenThrow(new IllegalArgumentException("Inválido input"));

        mockMvc.perform(get("/hello"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Inválido input"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleGeneric_shouldReturn500_whenUnexpectedExceptionIsThrown() throws Exception {
        when(helloUseCase.execute()).thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/hello"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                // detalhe interno NÃO deve vazar para o cliente
                .andExpect(jsonPath("$.detail").value("Um erro inesperado ocorreu. Por favor, tente novamente mais tarde."))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
