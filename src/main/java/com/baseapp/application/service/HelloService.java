package com.baseapp.application.service;

import com.baseapp.domain.model.HelloMessage;
import com.baseapp.domain.port.in.HelloUseCase;
import com.baseapp.domain.port.out.HelloMessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Implementação do caso de uso HelloUseCase.
 *
 * Responsabilidade desta classe:
 *  - Orquestrar a lógica de negócio
 *  - Criar o objeto de domínio HelloMessage (text + timestamp)
 *  - Delegar a obtenção do texto ao port de saída HelloMessageProvider
 *
 * O que NÃO é responsabilidade desta classe:
 *  - Saber de onde o texto vem (DB, config, API externa — papel do adapter)
 *  - Saber como a resposta será serializada (papel do controller)
 */
@Service
public class HelloService implements HelloUseCase {

    private static final Logger log = LoggerFactory.getLogger(HelloService.class);

    private final HelloMessageProvider messageProvider;

    public HelloService(HelloMessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    @Override
    public HelloMessage execute() {
        log.debug("Executando HelloUseCase");
        String text = messageProvider.provideMessageText();
        HelloMessage message = new HelloMessage(text, Instant.now());
        log.debug("Mensagem de domínio construída: '{}'", message.message());
        return message;
    }
}
