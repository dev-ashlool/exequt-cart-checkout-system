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

SQL scripts on the classpath: `schema.sql` (cart tables in Phase 2), `data.sql` (optional seed data in later phases).

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
- **JPA:** Hibernate DDL `none` (tables come from `schema.sql`); dialect H2; `open-in-view: false`.
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
| **2** | Cart module: create cart, add/list items, domain rules (complete). Checkout API deferred to a later phase. |
| **3** | Order module and state machine. |
| **4** | Payment module, mock provider, webhooks and idempotency. |
| **5** | Tests, README updates, cleanup. |

Further phases are implemented after review.

## Cart APIs (Phase 2)

Base URL (default): `http://localhost:8080`

All success bodies use `GenericResponse<T>`: `error`, `statusCode`, `description`, and `data`.

### Create cart

`POST /carts` → **201 Created** with `Location: /carts/{id}` and `data` of type **CreateCartResponse** (new empty cart: `OPEN`, empty `items`).

```bash
curl -s -D - -X POST http://localhost:8080/carts -o -
```

### Add item

`POST /carts/{cartId}/items` → **200 OK**, `data` is **CartResponse** (full cart including line items).

- If the same `productId` already exists, **quantity is increased** and the line is repriced to the incoming `price`.

```bash
curl -s -X POST http://localhost:8080/carts/1/items \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"SKU-100\",\"quantity\":2,\"price\":19.99}"
```

### Get cart

`GET /carts/{cartId}` → **200 OK**, `data` is **CartResponse**.

```bash
curl -s http://localhost:8080/carts/1
```

### Validation (request body)

`AddCartItemRequest`: `productId` required (non-blank string); `quantity` and `price` required and **positive** (`@Positive`).

### Domain behaviour (not exposed as endpoints yet)

- **`checkout()`** on the cart aggregate sets status to **CHECKED_OUT** (locks the cart for a future checkout flow). There is **no** checkout HTTP endpoint in Phase 2.

## API error model

Errors return a `GeneralResponse` JSON body with `error: true`, a stable `statusCode` (from `ResultCode`), and a `description`. HTTP status aligns with the error class and code (e.g. `404` for not found, `409` for conflicts / duplicate events, `400` for validation).
