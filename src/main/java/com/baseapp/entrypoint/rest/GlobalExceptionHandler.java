package com.baseapp.entrypoint.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Tratamento global de exceções
 *
 * Evita:
 *  - vazamento de stacktrace para o cliente
 *  - respostas de erro em formato HTML (padrão do Tomcat)
 *  - status 500 para erros que deveriam ser 4xx
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Erros de validação ou regras de negócio explicitamente verificadas.
     * Ex: argumento nulo, formato inválido, etc.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Bad Request");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Catch-all para qualquer exceção não tratada explicitamente.
     * Loga a stack completa internamente, mas só devolve uma mensagem genérica ao cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception e) {
        log.error("Erro inesperado", e);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Um erro inesperado ocorreu. Por favor, tente novamente mais tarde."
        );
        problem.setType(URI.create("about:blank"));
        problem.setTitle("Erro Interno do Servidor");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.internalServerError().body(problem);
    }
}
