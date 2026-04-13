package com.baseapp.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Objeto de valor imutável que representa uma mensagem de saudação.
 *
 * O compact constructor garante invariantes do domínio — nenhum caller
 * consegue criar uma HelloMessage inválida.
 */
public record HelloMessage(String message, Instant timestamp) {

    public HelloMessage {
        Objects.requireNonNull(message, "A mensagem não pode ser nula");
        if (message.isBlank()) {
            throw new IllegalArgumentException("A mensagem não pode estar em branco");
        }
        Objects.requireNonNull(timestamp, "O timestamp não pode ser nulo");
    }
}
