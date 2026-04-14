package com.baseapp.infrastructure.adapter.in.rest;

import com.baseapp.domain.model.HelloMessage;
import com.baseapp.domain.port.in.HelloUseCase;
import com.baseapp.infrastructure.adapter.in.rest.dto.HelloResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapter de entrada (driving adapter) — recebe requisições HTTP e delega
 * ao caso de uso correspondente.
 *
 * Responsabilidades:
 * - Receber e validar parâmetros HTTP
 * - Chamar o use case via porta de entrada (HelloUseCase)
 * - Converter o objeto de domínio para DTO de resposta (HelloResponse)
 * - Deixar a lógica de negócio e composição do domínio para o HelloService
 */
@Tag(name = "Hello", description = "Endpoint de saudação do microserviço")
@RestController
@RequestMapping("/hello")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    private final HelloUseCase helloUseCase;

    public HelloController(HelloUseCase helloUseCase) {
        this.helloUseCase = helloUseCase;
    }

    @Operation(summary = "Retorna uma mensagem de saudação", description = "Retorna uma mensagem gerada pelo serviço com o texto e o timestamp de geração.")
    @ApiResponse(responseCode = "200", description = "Mensagem de saudação retornada com sucesso", content = @Content(schema = @Schema(implementation = HelloResponse.class)))
    @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        log.info("GET /hello");
        HelloMessage message = helloUseCase.execute();
        return ResponseEntity.ok(HelloResponse.from(message));
    }
}
