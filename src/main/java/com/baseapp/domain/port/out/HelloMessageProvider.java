package com.baseapp.domain.port.out;

/**
 * Driven port (porta de saída) — define o contrato que o domínio exige
 * de um recurso externo. Implementado por HelloAdapter na camada de infra.
 *
 * O retorno é String (texto bruto) porque a responsabilidade de compor o
 * objeto de domínio HelloMessage (com timestamp) é do HelloService,
 * não do adapter. O adapter apenas fornece o dado — quem monta o domínio
 * é a camada de aplicação.
 */
public interface HelloMessageProvider {

    String provideMessageText();
}
