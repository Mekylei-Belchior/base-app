# AI Context — base-app

> For system architecture, ADRs, and deployment topology, see [/docs/architecture.md](architecture.md).

---
## Quick Reference

- **Package root:** `com.baseapp` | Java 21 | Spring Boot 3.4.5
- **Layer rule:** `domain` and `application` have **zero** imports from `infrastructure`
- **Coverage gate:** 80% LINE (JaCoCo)

```
domain.model          → immutable records (value objects)
domain.port.in        → driving port interfaces (use cases)
domain.port.out       → driven port interfaces (outbound contracts)
application.service   → @Service implementing use case ports
infrastructure.adapter.in.rest → @RestController + DTOs
infrastructure.adapter.out     → @Component adapters (DB, APIs, config)
infrastructure.config          → @Configuration classes
```

---
## Code Patterns

**Domain model** — `record` + compact constructor validation:
```java
public record HelloMessage(String message, Instant timestamp) {
    public HelloMessage {
        Objects.requireNonNull(message, "message is required");
        if (message.isBlank()) throw new IllegalArgumentException("message cannot be blank");
        Objects.requireNonNull(timestamp, "timestamp is required");
    }
}
```

**Ports** — plain interfaces, no Spring:
```java
public interface HelloUseCase { HelloMessage execute(); }
public interface HelloMessageProvider { String provideMessageText(); } // raw data only
```
**Service** — `@Service`, constructor injection, builds domain object:
```java
@Service
public class HelloService implements HelloUseCase {
    private static final Logger log = LoggerFactory.getLogger(HelloService.class);
    private final HelloMessageProvider messageProvider;
    public HelloService(HelloMessageProvider messageProvider) { this.messageProvider = messageProvider; }

    @Override
    public HelloMessage execute() {
        log.debug("Executando HelloUseCase");
        return new HelloMessage(messageProvider.provideMessageText(), Instant.now());
    }
}
```

**Controller** — injects use case, returns DTO:
```java
@RestController
@RequestMapping("/hello")
public class HelloController {
    private static final Logger log = LoggerFactory.getLogger(HelloController.class);
    private final HelloUseCase helloUseCase;
    public HelloController(HelloUseCase helloUseCase) { this.helloUseCase = helloUseCase; }

    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        log.info("GET /hello");
        return ResponseEntity.ok(HelloResponse.from(helloUseCase.execute()));
    }
}
```

**DTO** — `record` + static factory:
```java
public record HelloResponse(String message, String timestamp) {
    public static HelloResponse from(HelloMessage d) {
        return new HelloResponse(d.message(), d.timestamp().toString()); // d.message() not d.getMessage()
    }
}
```

**Outbound adapter** — `@Component implements` driven port:
```java
@Component
public class HelloAdapter implements HelloMessageProvider {
    @Override
    public String provideMessageText() { return "Hello from k3s 🚀"; }
    // may use @Value, JPA, WebClient — infrastructure stays here
}
```

**Exception handler** — `ProblemDetail`, never leak internals:
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ProblemDetail> handle(IllegalArgumentException e) {
    log.warn("bad request: {}", e.getMessage());
    ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    p.setType(URI.create("about:blank"));
    p.setProperty("timestamp", Instant.now());
    return ResponseEntity.badRequest().body(p);
}
// 500 handler: always return generic message — never e.getMessage()
```

**Logging:**
```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.debug("detail");  log.info("GET /x id={}", id);  log.warn("msg: {}", msg);  log.error("error", ex);
// NEVER System.out.println
```

---
## Testing Patterns

**Service** — no Spring context:
```java
@ExtendWith(MockitoExtension.class)
class HelloServiceTest {
    @Mock HelloMessageProvider messageProvider;
    HelloService sut;
    @BeforeEach void setUp() { sut = new HelloService(messageProvider); }

    @Test
    void execute_shouldReturnMessage_whenProviderReturnsText() {
        when(messageProvider.provideMessageText()).thenReturn("Hello");
        HelloMessage result = sut.execute();
        assertThat(result.message()).isEqualTo("Hello");   // record accessor, no "get"
        assertThat(result.timestamp()).isNotNull();
        verify(messageProvider, times(1)).provideMessageText();
    }
}
```

**Controller + exception handler** — `@WebMvcTest`, `@MockitoBean` (not `@MockBean`):
```java
@WebMvcTest(HelloController.class)           // loads @RestControllerAdvice automatically
class HelloControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean HelloUseCase helloUseCase;

    @Test
    void getHello_shouldReturn200() throws Exception {
        when(helloUseCase.execute()).thenReturn(new HelloMessage("Hi", Instant.now()));
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hi"));
    }

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

**Test naming:** `method_shouldBehavior_whenCondition()`

---
## Build

```bash
./gradlew test                            # tests + JaCoCo report
./gradlew bootJar                         # → build/libs/base-app.jar
./gradlew jacocoTestCoverageVerification  # enforce 80% LINE gate
```

---
## DON'Ts (critical)

- ❌ `@Autowired` on fields — constructor injection only
- ❌ `@MockBean` — use `@MockitoBean` (Boot 3.4+)
- ❌ `infrastructure.*` imports in `domain.*` or `application.*`
- ❌ Spring annotations (`@Transactional`, `@Service`, etc.) on domain interfaces
- ❌ Return domain objects from controllers — always map to DTO
- ❌ Business logic in controllers or adapters
- ❌ `System.out.println` — SLF4J only
- ❌ `e.getMessage()` in 500 handlers — return generic message
- ❌ Record accessors with `get` prefix — `record.name()` not `record.getName()`
- ❌ Tests without assertions

---
## New Feature Checklist

1. `domain/model/Foo.java` — record + compact constructor
2. `domain/port/in/FooUseCase.java` — plain interface
3. `domain/port/out/FooRepository.java` — plain interface (if data needed)
4. `application/service/FooService.java` — `@Service implements FooUseCase`
5. `infrastructure/adapter/out/FooAdapter.java` — `@Component implements FooRepository`
6. `infrastructure/adapter/in/rest/dto/FooResponse.java` — record + `from(Foo)`
7. `infrastructure/adapter/in/rest/FooController.java` — `@RestController`
8. `FooServiceTest` (`@ExtendWith(MockitoExtension.class)`) + `FooControllerTest` (`@WebMvcTest`)
