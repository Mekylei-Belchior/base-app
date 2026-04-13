package com.baseapp.domain.port.in;

import com.baseapp.domain.model.HelloMessage;

/**
 * Driving port (porta de entrada) — define o que o mundo externo pode
 * pedir ao domínio. Implementado por HelloService na camada de aplicação.
 */
public interface HelloUseCase {

    HelloMessage execute();
}
