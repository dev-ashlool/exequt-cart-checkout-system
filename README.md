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

SQL scripts on the classpath: `schema.sql` (cart, order, and payment tables), `data.sql` (optional seed data in later phases).

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

Use the **Maven Wrapper** (no local Maven installation required):

```powershell
.\mvnw.cmd clean test
```

```powershell
.\mvnw.cmd spring-boot:run
```

On Unix/macOS use `./mvnw` with the same goals. If Maven is installed globally, `mvn test` and `mvn spring-boot:run` are equivalent.

## Implementation phases

| Phase | Scope |
|-------|--------|
| **1** | Project foundation: Maven, packages, `common` response model and exception handling, H2/JPA config, placeholder SQL, README. |
| **2** | Cart module: create cart, add/list items, domain rules (complete). Checkout API deferred to a later phase. |
| **3** | Order module, checkout flow, idempotent duplicate checkout, order state machine (payment wiring in Phase 4). |
| **4** | Payment module, mock provider, webhooks and idempotency. |
| **5** | Tests, README updates, cleanup. |

Further phases are implemented incrementally after review and validation.

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

### Checkout (Phase 3)

`POST /carts/{cartId}/checkout` — creates an **order** from the cart, copies lines as immutable **order item** snapshots, computes **totalAmount**, locks the cart (**CHECKED_OUT**), and returns **`GenericResponse<OrderResponse>`**.

| Outcome | HTTP | Notes |
|--------|------|--------|
| First successful checkout | **201 Created** | `Location: /orders/{orderId}` |
| Duplicate / retry checkout for the same cart | **200 OK** | Same order as the first checkout; **no second order** (DB `UNIQUE` on `orders.cart_id` + cart row lock) |

Flow (orchestrated in **`CheckoutApplicationService`**, boundary via **`OrderCommandService`** — cart code does **not** use **`OrderRepository`**):

The checkout flow is intentionally idempotent.

1. **Pessimistic lock** the cart row (`findByIdForUpdate`).
2. If an order already exists for **`cartId`**, return that order with **200**.
3. Otherwise require cart **OPEN**, **non-empty** cart; build **`CreateOrderItemCommand`** lines from cart items.
4. **`OrderCommandService.createOrderFromCartSnapshot`** persists the order + line snapshots.
5. **`cart.checkout()`** then **`save(cart)`** in the **same transaction**.

```bash
curl -s -D - -X POST http://localhost:8080/carts/1/checkout -o -
```

### Orders (Phase 3)

`GET /orders/{orderId}` → **200 OK**, `data` is **OrderResponse** (includes `items`, `totalAmount`, `status`, `cartId`, timestamps).

```bash
curl -s http://localhost:8080/orders/1
```

### Order status machine (domain)

States: **CREATED** → **PENDING_PAYMENT** → **PAID** (success) or **PAYMENT_FAILED**; **PAYMENT_FAILED** → **PENDING_PAYMENT** again via **`startPayment()`**. **PAID** is terminal (further transitions throw **`ConflictException`**).

Late or conflicting payment events must not mutate a **PAID** order.


| Method | Allowed from | New status |
|--------|----------------|------------|
| **`startPayment()`** | CREATED, PAYMENT_FAILED | PENDING_PAYMENT |
| **`markPaid()`** | PENDING_PAYMENT | PAID |
| **`markPaymentFailed()`** | PENDING_PAYMENT | PAYMENT_FAILED |

Checkout creates orders in **CREATED** only; **payment** endpoints and **`startPayment()`** / **`markPaid()`** / **`markPaymentFailed()`** are reserved for **Phase 4** (no payment module in Phase 3).

### Domain behaviour (cart)

- **`checkout()`** on the cart aggregate sets status to **CHECKED_OUT** and is invoked from the checkout application service after the order is persisted.

## Payment module (Phase 4)

Payment logic lives under **`com.exequt.payment`**. Order updates go only through **`OrderCommandService`** (no `OrderRepository` in payment).

### Payment APIs

| Method | Path | Notes |
|--------|------|--------|
| **POST** | `/orders/{orderId}/payment/start` | Starts or resumes payment for an order; returns **`GenericResponse<PaymentAttemptResponse>`**. **201 Created** when a new **`INITIATED`** attempt is created (with `Location` hinting at the attempt); **200 OK** when an active **`INITIATED`** attempt already exists for that order. |
| **POST** | `/payments/mock-provider/webhook` | Mock provider callback; body includes `providerEventId`, `paymentAttemptId`, `status` (**CONFIRMED** or **FAILED**), and `amount`. Returns **`GenericResponse<WebhookResponse>`** with a **`processingResult`** (`PROCESSED`, `DUPLICATE`, `IGNORED`, or `REJECTED`). |

### Payment flow summary

1. **Payment start:** order must be payable (**CREATED** or **PAYMENT_FAILED**). If an active **INITIATED** attempt already exists, the existing attempt is returned with **200 OK**. Otherwise, the service locks the order row, transitions the order to **PENDING_PAYMENT**, and creates a new **INITIATED** **`PaymentAttempt`** (amount matches order total; mock **`providerReference`**).2. **INITIATED attempt:** only **INITIATED** attempts accept a terminal webhook; **`confirm()`** / **`fail()`** move the attempt to **CONFIRMED** / **FAILED** (terminal, immutable thereafter).
3. **Webhook handling:** **CONFIRMED** calls **`markOrderAsPaid`** → order **PAID**; **FAILED** calls **`markOrderPaymentFailed`** → order **PAYMENT_FAILED** (only from **PENDING_PAYMENT**).
4. **Retry:** when the order is **PAYMENT_FAILED**, a new **payment start** creates a **new** attempt and moves the order back to **PENDING_PAYMENT**; older attempts stay unchanged.
5. **PAID** is **final**; no further order or attempt mutations from payment paths.

### Webhook idempotency

- **Duplicate `providerEventId`:** a second delivery with the same id returns **HTTP 200** with **`processingResult: DUPLICATE`** and does **not** change order or attempt state again.
- **First terminal result wins:** the first **CONFIRMED** or **FAILED** webhook for an **INITIATED** attempt applies; later terminal payloads for the same attempt are stored for audit with **`IGNORED`** and do **not** mutate order or attempt.
- **Conflicting late events:** after an attempt is already **CONFIRMED** or **FAILED**, an opposite-status webhook is still accepted as **HTTP 200** but recorded as **`IGNORED`** (no business rollback).

### Payment integration tests

`PaymentFlowIntegrationTest` (MockMvc, full context) covers:

- Successful payment (start → **CONFIRMED** webhook → order **PAID**)
- Failed payment, retry, then success
- Duplicate webhook (**same `providerEventId`**)
- Duplicate payment start (returns existing **INITIATED** attempt)
- Conflicting webhook after a terminal result (**IGNORED**)

## API error model

Errors return a `GeneralResponse` JSON body with `error: true`, a stable `statusCode` (from `ResultCode`), and a `description`. HTTP status aligns with the error class and code (e.g. `404` for not found, `409` for conflicts / duplicate events, `400` for validation).
