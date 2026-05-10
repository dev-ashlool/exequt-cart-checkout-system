# ExeQut Cart Checkout + Mock Payment System

Backend service for a small e-commerce flow: create a cart, add items, checkout to an order, and pay via a mock payment provider with idempotent webhook handling.

## Architecture

- **Style:** single deployable **modular monolith** with **package-by-feature** boundaries (`cart`, `order`, `payment`, `common`).
- **Inside each feature:** layered packages — `api` (controllers), `api.dto` (API contracts), `application` (orchestration and transactions), `domain` (model and invariants), `persistence` (JPA and repositories).
- **Common:** shared technical concerns only (response wrappers, result codes, exceptions, global exception handler). No business rules in `common`.

See [docs/architecture-decisions.md](docs/architecture-decisions.md) for full principles and module ownership rules.

## Technology stack

| Area | Choice |
|------|--------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.x |
| Build | Maven |
| Web | Spring Web |
| Persistence | Spring Data JPA |
| Database | H2 (development / in-memory) |
| Validation | `spring-boot-starter-validation` |
| Boilerplate | Lombok |
| Tests | JUnit 5, Spring Boot Test |

SQL scripts on the classpath: `schema.sql`, `data.sql` (placeholders in Phase 1; real DDL/DML in later phases).

## Package structure

```text
com.exequt
├── ExeQutApplication
├── cart
│   ├── api
│   ├── api.dto
│   ├── application
│   ├── domain
│   └── persistence
├── order
│   ├── api
│   ├── api.dto
│   ├── application
│   ├── domain
│   └── persistence
├── payment
│   ├── api
│   ├── api.dto
│   ├── application
│   ├── domain
│   └── persistence
└── common
    ├── response
    ├── exception
    ├── handler
    ├── constants
    └── util
```

## Configuration

- **Application name:** `exequt-cart-checkout` (`spring.application.name`).
- **H2:** in-memory database, console enabled at `/h2-console`.
- **JPA:** Hibernate DDL set to `none` until domain entities are introduced; dialect H2; `open-in-view: false`.
- **SQL init:** enabled so `schema.sql` / `data.sql` run on startup.
- **Logging:** `INFO` root, `DEBUG` for `com.exequt`.

## Run

```bash
mvn spring-boot:run
```

```bash
mvn test
```

## Implementation phases

| Phase | Scope |
|-------|--------|
| **1** | Project foundation: Maven, packages, `common` response model and exception handling, H2/JPA config, placeholder SQL, README. |
| **2** | Cart module (API, application, domain, persistence). |
| **3** | Order module and state machine. |
| **4** | Payment module, mock provider, webhooks and idempotency. |
| **5** | Tests, README updates, cleanup. |

Phase 1 stops here; further phases are implemented after review.

## API error model

Errors return a `GeneralResponse` JSON body with `error: true`, a stable `statusCode` (from `ResultCode`), and a `description`. HTTP status aligns with the error class and code (e.g. `404` for not found, `409` for conflicts / duplicate events, `400` for validation).
