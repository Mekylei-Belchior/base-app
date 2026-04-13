package com.baseapp.infrastructure.adapter.in.rest;

import com.baseapp.domain.model.HelloMessage;
import com.baseapp.domain.port.in.HelloUseCase;
import com.baseapp.infrastructure.adapter.in.rest.dto.HelloResponse;
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
 *  - Receber e validar parâmetros HTTP
 *  - Chamar o use case via porta de entrada (HelloUseCase)
 *  - Converter o objeto de domínio para DTO de resposta (HelloResponse)
 *  - Deixar a lógica de negócio e composição do domínio para o HelloService
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    private final HelloUseCase helloUseCase;

    public HelloController(HelloUseCase helloUseCase) {
        this.helloUseCase = helloUseCase;
    }

    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        log.info("GET /hello");
        HelloMessage message = helloUseCase.execute();
        return ResponseEntity.ok(HelloResponse.from(message));
    }
}
