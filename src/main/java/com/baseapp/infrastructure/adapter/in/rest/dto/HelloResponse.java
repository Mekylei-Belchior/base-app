package com.baseapp.infrastructure.adapter.in.rest.dto;

import com.baseapp.domain.model.HelloMessage;

/**
 * DTO de resposta da API REST.
 *
 * Converte o objeto de domínio HelloMessage para uma representação
 * adequada para serialização JSON. Isolado do domínio: mudanças no
 * contrato da API não afetam HelloMessage e vice-versa.
 *
 * Acessores de record: message() e timestamp() — sem prefixo "get".
 */
public record HelloResponse(String message, String timestamp) {

    public static HelloResponse from(HelloMessage domain) {
        return new HelloResponse(
                domain.message(),
                domain.timestamp().toString()
        );
    }
}
