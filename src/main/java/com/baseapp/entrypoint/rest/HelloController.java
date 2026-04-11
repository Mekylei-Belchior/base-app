package com.baseapp.entrypoint.rest;

import com.baseapp.application.usecase.HelloUseCase;
import com.baseapp.domain.model.HelloMessage;
import com.baseapp.entrypoint.rest.dto.HelloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    private final HelloUseCase helloUseCase;

    public HelloController(HelloUseCase helloUseCase) {
        this.helloUseCase = helloUseCase;
    }

    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        HelloMessage message = helloUseCase.execute();
        return ResponseEntity.ok(HelloResponse.from(message));
    }
}
