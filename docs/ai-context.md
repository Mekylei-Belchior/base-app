# AI Context — base-app

> For system architecture, ADRs, and deployment topology, see [/docs/architecture.md](architecture.md).

---

## Quick Reference

- **Package root:** `com.baseapp`
- **Java:** 21 | **Spring Boot:** 3.4.5 | **Build:** Gradle
- **Layer rule:** `domain` and `application` have **zero** imports from `infrastructure`
- **Coverage gate:** 80% LINE minimum (JaCoCo)

### Package map
```
domain.model          → immutable records (value objects)
domain.port.in        → driving port interfaces (use cases)
domain.port.out       → driven port interfaces (outbound contracts)
application.service   → @Service classes implementing use case ports
infrastructure.adapter.in.rest  → @RestController + DTOs
infrastructure.adapter.out      → @Component adapters (DB, config, APIs)
infrastructure.config           → @Configuration classes
```

---

## Code Patterns

### Domain model — use `record` with compact constructor validation

```java
// DO
public record HelloMessage(String message, Instant timestamp) {
    public HelloMessage {
        Objects.requireNonNull(message, "A mensagem não pode ser nula");
        if (message.isBlank()) throw new IllegalArgumentException("...");
        Objects.requireNonNull(timestamp, "...");
    }
}

// DON'T — mutable class or no validation
public class HelloMessage {
    private String message; // ❌ mutable
}
```

### Ports — plain interfaces, no Spring annotations

```java
// DO — driving port
public interface HelloUseCase {
    HelloMessage execute();
}

// DO — driven port
public interface HelloMessageProvider {
    String provideMessageText(); // returns raw data; domain object is built in the service
}

// DON'T — Spring annotations on domain interfaces
public interface HelloUseCase {
    @Transactional // ❌ infrastructure concern in domain
    HelloMessage execute();
}
```

### Application service — constructor injection, implements use case

```java
// DO
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
        return new HelloMessage(text, Instant.now());
    }
}

// DON'T — inject repository/JPA directly into service
@Service
public class HelloService {
    @Autowired // ❌ field injection
    private SomeRepository repo;
}
```

### REST controller — delegates to use case, maps domain → DTO

```java
// DO
@RestController
@RequestMapping("/hello")
public class HelloController {
    private final HelloUseCase helloUseCase;

    public HelloController(HelloUseCase helloUseCase) { // constructor injection
        this.helloUseCase = helloUseCase;
    }

    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        log.info("GET /hello");
        HelloMessage message = helloUseCase.execute();
        return ResponseEntity.ok(HelloResponse.from(message)); // domain → DTO here
    }
}

// DON'T — business logic in controller, return domain object directly
@GetMapping
public HelloMessage hello() { // ❌ exposes domain type
    return new HelloMessage("...", Instant.now()); // ❌ logic belongs in service
}
```

### DTO — `record` with a static factory `from(DomainObject)`

```java
// DO
public record HelloResponse(String message, String timestamp) {
    public static HelloResponse from(HelloMessage domain) {
        return new HelloResponse(domain.message(), domain.timestamp().toString());
    }
}

// Record accessor syntax: domain.message() — NOT domain.getMessage()
```

### Outbound adapter — implements driven port, may use Spring

```java
// DO
@Component
public class HelloAdapter implements HelloMessageProvider {
    @Override
    public String provideMessageText() {
        return "Hello from k3s 🚀"; // or DB/config/remote call
    }
}
// May use @Value, JPA, WebClient etc. — infrastructure concerns belong here
```

### Exception handling — `@RestControllerAdvice` + `ProblemDetail`

```java
// DO — use RFC 9457 ProblemDetail, never leak stack traces
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException e) {
    log.warn("Erro de requisição: {}", e.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problem.setType(URI.create("about:blank"));
    problem.setTitle("Requisição Inválida");
    problem.setProperty("timestamp", Instant.now());
    return ResponseEntity.badRequest().body(problem);
}

// DON'T — expose internal detail on 500
@ExceptionHandler(Exception.class)
public ResponseEntity<String> handleError(Exception e) {
    return ResponseEntity.internalServerError().body(e.getMessage()); // ❌ leaks internals
}
```

### Logging — SLF4J, no System.out

```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

log.debug("Executando lógica X");           // trace/debug
log.info("POST /resource id={}", id);       // HTTP entry points
log.warn("Dado inválido: {}", msg);         // recoverable bad input
log.error("Erro inesperado", exception);    // unexpected — include exception
// NEVER: System.out.println(...)
```

---

## Testing Patterns

### Service unit test — `@ExtendWith(MockitoExtension.class)`, no Spring context

```java
@ExtendWith(MockitoExtension.class)
class HelloServiceTest {

    @Mock HelloMessageProvider messageProvider;
    HelloService sut;   // system under test

    @BeforeEach
    void setUp() { sut = new HelloService(messageProvider); }

    @Test
    void execute_shouldReturnMessageWithTextFromProvider() {
        when(messageProvider.provideMessageText()).thenReturn("Hello");
        HelloMessage result = sut.execute();
        assertThat(result.message()).isEqualTo("Hello");  // record accessor, no "get"
        assertThat(result.timestamp()).isNotNull();
        verify(messageProvider, times(1)).provideMessageText();
    }
}
```

### Controller test — `@WebMvcTest`, mock use case with `@MockitoBean`

```java
@WebMvcTest(HelloController.class)
class HelloControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean HelloUseCase helloUseCase;  // NOT @MockBean (deprecated in Boot 3.4)

    @Test
    void getHello_shouldReturn200WithJson() throws Exception {
        when(helloUseCase.execute()).thenReturn(new HelloMessage("Hi", Instant.now()));
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Hi"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
```

### Exception handler test — trigger via controller, no direct instantiation

```java
// @WebMvcTest loads @RestControllerAdvice automatically
@WebMvcTest(HelloController.class)
class GlobalExceptionHandlerTest {
    @MockitoBean HelloUseCase helloUseCase;

    @Test
    void whenIllegalArgument_shouldReturn400() throws Exception {
        when(helloUseCase.execute()).thenThrow(new IllegalArgumentException("bad"));
        mockMvc.perform(get("/hello"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("bad"));
    }
}
```

### Test naming convention

```
methodName_shouldExpectedBehavior_whenCondition()
// e.g.: execute_shouldReturnMessage_whenProviderReturnsText
// e.g.: execute_shouldDelegateToProviderExactlyOnce
```

---

## Build Commands

```bash
./gradlew test                            # compile + run tests + JaCoCo HTML report
./gradlew bootJar                         # fat JAR → build/libs/base-app.jar
./gradlew jacocoTestCoverageVerification  # enforce 80% LINE coverage gate
```

---

## Configuration Reference (for code generation)

- Active profile: `application.yml` (default) + `application-prod.yml` (prod overrides)
- Env var to set environment: `APP_ENV=dev|staging|prod`
- Prod disables Swagger (`springdoc.swagger-ui.enabled: false`) and `/v3/api-docs`
- Health details: always `show-details: never`
- Actuator exposed endpoints: `health`, `info`, `prometheus`

---

## DOs and DON'Ts

### Layer rules
- **DO** keep `domain.model` and `domain.port.*` free of any Spring annotations
- **DO** add new use cases: interface in `domain.port.in` → `@Service` in `application.service`
- **DO** add new outbound adapters: interface in `domain.port.out` → `@Component` in `infrastructure.adapter.out`
- **DON'T** import `infrastructure.*` anywhere in `domain.*` or `application.*`
- **DON'T** put business logic in controllers or adapters

### Code style
- **DO** use `record` for all value objects and DTOs
- **DO** use constructor injection everywhere — never `@Autowired` on fields
- **DO** use SLF4J: `LoggerFactory.getLogger(MyClass.class)` — never `System.out`
- **DO** return `ResponseEntity<T>` from all controller methods
- **DON'T** use `@MockBean` — use `@MockitoBean` (Spring Boot 3.4+)
- **DON'T** add `get` prefix to record accessors — `record.message()` not `record.getMessage()`

### Security
- **DON'T** log credentials, tokens, or PII
- **DON'T** return exception messages or stack traces in 500 responses
- **DO** validate domain invariants in compact constructors (`Objects.requireNonNull`, `IllegalArgumentException`)
- **DO** use `ProblemDetail` for all exception handler responses — never raw strings

### Testing
- **DO** test services with `@ExtendWith(MockitoExtension.class)` — no Spring context loaded
- **DO** test controllers with `@WebMvcTest` — web layer only
- **DO** use `assertThat(...)` from AssertJ — not JUnit `assertEquals`
- **DON'T** start full `ApplicationContext` in unit tests
- **DON'T** write tests without assertions

---

## New Feature Checklist

Steps to add a new domain feature (e.g., `Greeting`):

1. `domain/model/Greeting.java` — record with compact constructor validation
2. `domain/port/in/GreetingUseCase.java` — plain interface (no annotations)
3. `domain/port/out/GreetingRepository.java` — plain interface, if outbound data needed
4. `application/service/GreetingService.java` — `@Service implements GreetingUseCase`, constructor-injects outbound port
5. `infrastructure/adapter/out/GreetingAdapter.java` — `@Component implements GreetingRepository`
6. `infrastructure/adapter/in/rest/dto/GreetingResponse.java` — record with `static from(Greeting domain)`
7. `infrastructure/adapter/in/rest/GreetingController.java` — `@RestController`, injects `GreetingUseCase`
8. Tests: `GreetingServiceTest` (`@ExtendWith(MockitoExtension.class)`) + `GreetingControllerTest` (`@WebMvcTest`)
