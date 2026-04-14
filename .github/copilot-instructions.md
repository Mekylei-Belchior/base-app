# Copilot Instructions — base-app

> Read `docs/ai-context.md` for the full reference. This file is the fast-path summary optimized for Copilot autocomplete.

---

## Project: base-app

Java 21 + Spring Boot 3.4 microservice. **Hexagonal Architecture** (Ports & Adapters).  
Package root: `com.baseapp`

---

## Layer Rules (strict)

| Layer | Package | Rule |
|-------|---------|------|
| Domain | `domain.model`, `domain.port.*` | No Spring. Records only. No infra imports. |
| Application | `application.service` | `@Service`, constructor injection, implements use case port. |
| Infrastructure | `infrastructure.adapter.*`, `infrastructure.config` | May use Spring freely. |

---

## Patterns to Always Follow

**Domain model** — immutable record, validate in compact constructor:
```java
public record Foo(String name, Instant createdAt) {
    public Foo {
        Objects.requireNonNull(name, "name is required");
        if (name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
    }
}
```

**Use case port** — plain interface, no annotations:
```java
public interface FooUseCase { Foo execute(String input); }
```

**Outbound port** — returns raw data, not domain objects:
```java
public interface FooRepository { String findRawData(String id); }
```

**Service** — constructor injection, builds domain object:
```java
@Service
public class FooService implements FooUseCase {
    private static final Logger log = LoggerFactory.getLogger(FooService.class);
    private final FooRepository repo;
    public FooService(FooRepository repo) { this.repo = repo; }
    @Override
    public Foo execute(String input) {
        log.debug("executing FooUseCase input={}", input);
        return new Foo(repo.findRawData(input), Instant.now());
    }
}
```

**Controller** — injects use case port, maps domain → DTO:
```java
@RestController
@RequestMapping("/foo")
public class FooController {
    private static final Logger log = LoggerFactory.getLogger(FooController.class);
    private final FooUseCase fooUseCase;
    public FooController(FooUseCase fooUseCase) { this.fooUseCase = fooUseCase; }

    @GetMapping("/{id}")
    public ResponseEntity<FooResponse> get(@PathVariable String id) {
        log.info("GET /foo/{}", id);
        return ResponseEntity.ok(FooResponse.from(fooUseCase.execute(id)));
    }
}
```

**DTO** — record with static factory:
```java
public record FooResponse(String name, String createdAt) {
    public static FooResponse from(Foo domain) {
        return new FooResponse(domain.name(), domain.createdAt().toString());
    }
}
```

**Exception handler** — always use `ProblemDetail`, never leak internals:
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ProblemDetail> handle(IllegalArgumentException e) {
    log.warn("bad request: {}", e.getMessage());
    ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    p.setType(URI.create("about:blank"));
    p.setProperty("timestamp", Instant.now());
    return ResponseEntity.badRequest().body(p);
}
```

---

## Testing

**Service unit test** — no Spring context:
```java
@ExtendWith(MockitoExtension.class)
class FooServiceTest {
    @Mock FooRepository repo;
    FooService sut;
    @BeforeEach void setUp() { sut = new FooService(repo); }

    @Test
    void execute_shouldReturnFoo_whenRepoReturnsData() {
        when(repo.findRawData("x")).thenReturn("bar");
        Foo result = sut.execute("x");
        assertThat(result.name()).isEqualTo("bar");
        verify(repo, times(1)).findRawData("x");
    }
}
```

**Controller test** — `@WebMvcTest` + `@MockitoBean` (not `@MockBean`):
```java
@WebMvcTest(FooController.class)
class FooControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean FooUseCase fooUseCase;

    @Test
    void get_shouldReturn200() throws Exception {
        when(fooUseCase.execute("1")).thenReturn(new Foo("bar", Instant.now()));
        mockMvc.perform(get("/foo/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("bar"));
    }
}
```

---

## Critical DON'Ts

- DON'T use `@Autowired` on fields — constructor injection only
- DON'T use `@MockBean` — use `@MockitoBean` (Boot 3.4+)
- DON'T import `infrastructure.*` in `domain.*` or `application.*`
- DON'T return domain objects from controllers — always map to DTO
- DON'T call `System.out.println` — SLF4J only
- DON'T expose stack traces or internal messages in HTTP 500 responses
- DON'T call record accessor with `get` prefix — `record.name()` not `record.getName()`
- DON'T add `@Transactional` or Spring annotations to domain interfaces

---

## Logging convention

```java
private static final Logger log = LoggerFactory.getLogger(ThisClass.class);
log.debug("detail for trace");
log.info("GET /resource id={}", id);
log.warn("recoverable issue: {}", msg);
log.error("unexpected error", exception);
```

---

## Build commands

```bash
./gradlew test                            # tests + JaCoCo report
./gradlew bootJar                         # fat JAR → build/libs/base-app.jar
./gradlew jacocoTestCoverageVerification  # enforce 80% line coverage
```
