package com.baseapp.infrastructure.adapter.out;

import com.baseapp.domain.port.out.HelloMessageProvider;
import org.springframework.stereotype.Component;

/**
 * Adapter de saída (driven adapter) — implementa HelloMessageProvider.
 *
 * Aqui fica a lógica de busca de dados de fontes externas:
 * banco de dados, arquivo de configuração, API remota, etc.
 *
 * Por ser infraestrutura, pode depender de Spring (@Component, @Value, JPA, etc.).
 * A camada de domínio não sabe que este adapter existe — ela conhece apenas a
 * interface HelloMessageProvider definida em domain.port.out.
 */
@Component
public class HelloAdapter implements HelloMessageProvider {

    @Override
    public String provideMessageText() {
        return "Hello from k3s \uD83D\uDE80";
    }
}
