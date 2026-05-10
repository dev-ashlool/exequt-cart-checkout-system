You are an expert Java/Spring Boot Tech Lead and Senior Backend Architect.

I need you to implement a complete backend assignment for:

Cart Checkout + Mock Payment System
Single Service | Java / Spring Boot

This is a challenge-based hiring task for a Java Tech Lead role. The solution must be clean, simple, production-oriented, and easy to explain in a technical review.

==================================================
1. TASK CONTEXT
==================================================

We are designing the backend for a small e-commerce product.

The system allows users to:
1. Create a cart
2. Add items
3. Checkout
4. Pay for their order

The system is NOT expected to scale heavily, but:
- It handles real money
- Order state must always be correct
- No double payments
- No invalid state transitions
- Webhooks may arrive twice
- Duplicate events must not corrupt state

The assignment focuses on:
- Clear domain model
- Clear boundaries inside a single service
- Order state machine
- Business invariants
- Payment safety
- Webhook idempotency
- Duplicate/retry/race condition handling
- Conscious architectural trade-offs
- Future extensibility without overengineering

==================================================
2. TECHNOLOGY STACK
==================================================

Use:
- Java 17
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- H2 Database
- spring-boot-starter-validation
- Lombok
- JUnit 5 / Spring Boot Test

Use SQL files:
- schema.sql
- data.sql if needed

Do NOT use:
- Kafka
- RabbitMQ
- Distributed transactions
- Multiple microservices
- Maven multi-module
- Event sourcing
- CQRS
- Saga frameworks
- Heavy workflow engines
- MapStruct
- Complex hexagonal over-abstraction

Swagger/OpenAPI is optional. Do not prioritize it unless everything else is complete.

==================================================
3. ARCHITECTURE STYLE
==================================================

Build a single Spring Boot application as a package-based modular monolith.

Root package:
com.exequt

Use package-by-feature, NOT package-by-layer.

Required modules:
- com.exequt.cart
- com.exequt.order
- com.exequt.payment
- com.exequt.common

Each business module should internally contain:
- api
- api.dto
- application
- domain
- persistence

Common module should contain only shared technical concerns:
- response wrapper
- result codes
- exceptions
- global exception handler
- shared validation/error models
- small shared utilities/constants only

Common module must NOT become a dumping ground for business logic.

==================================================
4. DESIGN PATTERNS / PRINCIPLES
==================================================

Use these intentionally:

Patterns:
- Modular Monolith
- Layered Architecture
- Rich Domain Model
- Repository Pattern
- DTO Pattern
- Manual Mapper Pattern
- State Machine Pattern
- Idempotency Pattern
- Facade / Application Boundary Pattern
- Transaction Boundary / Unit of Work

Principles:
- Separation of Concerns
- Single Responsibility Principle
- Encapsulation
- High Cohesion / Low Coupling
- Tell, Don’t Ask
- Fail Fast
- Idempotency by Design
- KISS / YAGNI
- Future Service Ownership
- Dependency Discipline

Rules:
- Controllers must stay thin.
- Controllers must not contain business logic.
- Controllers must not access repositories directly.
- Application Services own orchestration and transaction boundaries.
- Domain entities own business invariants and state transitions.
- Repositories are accessed only from the owning module’s application/domain boundary.
- Never expose JPA entities directly through APIs.
- Use DTOs for all API input/output.
- Use manual mapper classes.
- Avoid unnecessary interfaces unless they protect module boundaries.

==================================================
5. MODULE OWNERSHIP RULES
==================================================

Cart module owns:
- Cart
- CartItem
- CartStatus
- cart lifecycle
- cart APIs
- cart persistence
- cart validation

Order module owns:
- Order
- OrderItem
- OrderStatus
- order state machine
- order lifecycle
- order business invariants
- order APIs
- order persistence

Payment module owns:
- PaymentAttempt
- PaymentWebhookEvent
- payment execution lifecycle
- payment retries
- webhook processing
- payment persistence

Cross-module rules:
- Cart module must NOT access OrderRepository directly.
- Payment module must NOT access OrderRepository directly.
- Cart module must NOT access PaymentAttemptRepository directly.
- Payment module communicates with Order only through an Order application boundary/command service.
- Cart checkout creates an order through an Order module application boundary/command service.

Example boundary:
- OrderCommandService.createOrderFromCart(...)
- OrderCommandService.startPayment(...)
- OrderCommandService.markOrderAsPaid(...)
- OrderCommandService.markOrderPaymentFailed(...)

==================================================
6. DOMAIN MODEL
==================================================

Use this small and clear domain model:

Cart:
- id: Long
- status: CartStatus
- items: List<CartItem>
- createdAt
- updatedAt

CartItem:
- id: Long
- cartId / cart reference
- productId
- quantity
- price
- lineTotal

Order:
- id: Long
- cartId
- status: OrderStatus
- totalAmount
- items: List<OrderItem>
- version for optimistic locking
- createdAt
- updatedAt

OrderItem:
- id: Long
- orderId / order reference
- productId
- quantity
- price
- lineTotal

PaymentAttempt:
- id: Long
- orderId
- amount
- status: PaymentAttemptStatus
- createdAt
- completedAt

PaymentWebhookEvent:
- id: Long
- providerEventId
- paymentAttemptId
- status/statusFromProvider
- amount
- processingResult
- failureReason / description if needed
- createdAt

Use:
- Long IDs
- @GeneratedValue(strategy = GenerationType.IDENTITY)
- BigDecimal for all money values
- precision = 19
- scale = 2
- Never use double for money

Use Lombok:
- @Getter
- @NoArgsConstructor
- @AllArgsConstructor only if useful
- Avoid @Data on entities
- Avoid exposing public setters for critical state when possible
- Direct state mutation must be avoided for important business state

==================================================
7. ENUMS
==================================================

CartStatus:
- OPEN
- CHECKED_OUT

OrderStatus:
- CREATED
- PENDING_PAYMENT
- PAYMENT_FAILED
- PAID

PaymentAttemptStatus:
- INITIATED
- CONFIRMED
- FAILED

WebhookProcessingResult:
- PROCESSED
- DUPLICATE
- IGNORED
- REJECTED

Webhook provider statuses:
- CONFIRMED
- FAILED

Do NOT implement CANCELLED in the first version.
CANCELLED is intentionally excluded because cancellation introduces refund/reversal rules and is a future extension.

==================================================
8. ORDER STATE MACHINE
==================================================

Required Order states:
- CREATED
- PENDING_PAYMENT
- PAYMENT_FAILED
- PAID

Valid transitions:
- CREATED -> PENDING_PAYMENT
- PENDING_PAYMENT -> PAID
- PENDING_PAYMENT -> PAYMENT_FAILED
- PAYMENT_FAILED -> PENDING_PAYMENT

Rules:
- PAID is final and immutable.
- No webhook may revert PAID.
- No payment retry is allowed after PAID.
- Invalid transitions must throw a business exception.
- No direct external mutation like order.setStatus(PAID).
- All transitions must go through explicit domain behavior.

Domain methods should include:
- order.startPayment()
- order.markPaid()
- order.markPaymentFailed()

PaymentAttempt methods should include:
- confirm()
- fail()

Cart methods should include:
- addItem(...)
- checkout()
- validateCanModify()
- validateNotEmpty()

==================================================
9. BUSINESS INVARIANTS
==================================================

Enforce these through domain validation, transactions, optimistic locking, and DB constraints:

Cart:
- Cart can be modified only before checkout.
- Checkout locks the cart.
- Empty carts cannot be checked out.
- Duplicate product added to cart increases quantity.
- Quantity must be positive.
- Price must be positive.

Checkout/Order:
- Checkout must not create duplicate orders.
- One cart can create only one order.
- Order items are snapshots copied from cart items.
- Order totals and item snapshots are immutable after checkout.
- Order starts in CREATED.
- Order state transitions must be valid.

Payment:
- Payment amount must match order total.
- PaymentAttempt represents each payment try.
- PaymentAttempt starts as INITIATED.
- Completed PaymentAttempts are immutable from a business perspective.
- Payment retry creates a new PaymentAttempt.
- Old completed attempts are never reused.
- Only one active INITIATED PaymentAttempt is allowed per Order at a time.
- First terminal result wins.
- Later conflicting webhook events must be recorded/audited if possible but ignored business-wise.

Webhook:
- providerEventId is required.
- providerEventId must be unique.
- Duplicate webhook with the same providerEventId must return 200 OK and not change business state again.
- Unknown paymentAttemptId must not corrupt state.
- Invalid status must be rejected.
- Amount mismatch must be rejected/recorded as REJECTED and must never change business state.
- Webhook processing must be transactional.
- Do not leave webhook event marked processed if payment/order update failed.

==================================================
10. REST API ENDPOINTS
==================================================

Implement exactly these main APIs:

Cart:
1. POST /carts
   - Create new cart
   - Return created cart

2. POST /carts/{cartId}/items
   - Add item to cart
   - Request: productId, quantity, price
   - If productId already exists in cart, increase quantity
   - Cart must be OPEN

3. GET /carts/{cartId}
   - Return cart details

Checkout:
4. POST /carts/{cartId}/checkout
   - Create order from cart
   - Lock cart
   - If order already exists for cart, return existing order
   - First checkout: 201 Created
   - Duplicate checkout retry: 200 OK

Order:
5. GET /orders/{orderId}
   - Return order details

Payment:
6. POST /orders/{orderId}/payment/start
   - Start payment attempt
   - Order must be CREATED or PAYMENT_FAILED
   - If active INITIATED attempt already exists, return existing attempt
   - First start: 201 Created
   - Duplicate active start: 200 OK

Webhook:
7. POST /payments/webhook
   - Request: providerEventId, paymentAttemptId, status, amount
   - Status allowed: CONFIRMED / FAILED
   - Must be idempotent

Mock Provider:
8. Implement minimal mock provider inside same project.
   It should be enough to demonstrate full flow:
   - Simulate payment start/result
   - Allow triggering payment result CONFIRMED or FAILED
   - Calls or delegates to /payments/webhook behavior
   - No complex simulation required

You may create a simple endpoint like:
POST /payments/mock-provider/trigger
Request:
- paymentAttemptId
- providerEventId
- status
- amount

This endpoint can internally call the same webhook application service directly rather than making an HTTP call to itself.

==================================================
11. API RESPONSE STYLE
==================================================

Use code style -like response style.

Create common response classes similar to:

Response:
- boolean error
- String statusCode
- String description

GeneralResponse extends Response

GenericResponse<T> extends Response:
- T data

ResultCode enum:
- SUCCESS
- BAD_REQUEST
- NOT_FOUND
- BUSINESS_CONFLICT
- VALIDATION_ERROR
- DUPLICATE_EVENT
- INTERNAL_ERROR

All controllers should return ResponseEntity.

Success example:
return new ResponseEntity<>(
    new GenericResponse<CartResponseDTO>(false, ResultCode.SUCCESS, response).buildResponse(),
    HttpStatus.OK
);

For create:
- use HttpStatus.CREATED where applicable

Error responses:
- must follow same response contract
- use GlobalExceptionHandler with @ControllerAdvice
- do not expose stack traces

HTTP mapping:
- 400 Bad Request: invalid request format or validation failure
- 404 Not Found: missing resource
- 409 Conflict: business rule violation, invalid state transition, optimistic locking conflict
- 500 Internal Server Error: unexpected error

==================================================
12. TRANSACTION RULES
==================================================

Use @Transactional only on application services.

Do NOT put @Transactional on controllers.

Required transactional use cases:
- create cart item / modify cart
- checkout
- start payment
- webhook processing

Checkout transaction:
- Load cart
- Validate cart exists
- Validate cart is OPEN
- Validate cart not empty
- Check existing order for cart
- If exists return existing order
- Otherwise create order through Order boundary
- Copy cart items into order items
- Lock cart
- Persist atomically

Start payment transaction:
- Load order through Order boundary
- Validate payable state CREATED or PAYMENT_FAILED
- Check active INITIATED payment attempt
- If exists return it
- Move order to PENDING_PAYMENT through Order boundary
- Create PaymentAttempt
- Persist atomically

Webhook transaction:
- Validate providerEventId
- Check duplicate providerEventId
- If duplicate return success acknowledgement
- Record webhook event
- Load PaymentAttempt
- Validate attempt exists
- Validate amount
- Validate status
- If attempt INITIATED:
  - apply result to PaymentAttempt
  - transition Order through Order boundary
  - mark webhook PROCESSED
- If attempt already terminal:
  - record/mark webhook IGNORED
  - do not mutate PaymentAttempt or Order

Avoid long external calls inside transactions.

==================================================
13. DATABASE RULES
==================================================

Use H2 for assignment.

Provide explicit SQL files:
- schema.sql
- data.sql if needed

Add constraints:
- unique constraint on orders.cart_id
- unique constraint on payment_webhook_events.provider_event_id
- not null constraints for critical fields
- foreign keys inside monolith
- indexes for lookup paths:
  - cart_id
  - order_id
  - payment_attempt_id
  - provider_event_id

Use optimistic locking:
- Add @Version to Order

Positive quantity/amount:
- enforce primarily through API/domain validation
- optional DB CHECK constraints if H2 supports cleanly

Note:
Active payment attempt uniqueness may be enforced mainly through application logic in this assignment.
If using PostgreSQL in future, partial unique index can enforce one active INITIATED attempt per order.

==================================================
14. LOGGING RULES
==================================================

Logging is mandatory.

Use Slf4j:
- @Slf4j

Add meaningful logs in:
- controllers entry points
- application services
- checkout
- payment start
- webhook processing
- duplicate webhook handling
- invalid webhook handling
- state transitions

Do not log sensitive data.
Do not spam logs.
Include useful identifiers:
- cartId
- orderId
- paymentAttemptId
- providerEventId

Examples:
log.info("Starting checkout for cartId={}", cartId);
log.info("Duplicate webhook received, providerEventId={}", providerEventId);
log.warn("Webhook rejected due to amount mismatch, providerEventId={}, paymentAttemptId={}", ...);

==================================================
15. VALIDATION RULES
==================================================

Use request DTO validation:
- @NotNull
- @NotBlank
- @Positive
- @Valid

Examples:
AddCartItemRequest:
- productId: @NotBlank or @NotNull depending type
- quantity: @NotNull @Positive
- price: @NotNull @Positive

WebhookRequest:
- providerEventId: @NotBlank
- paymentAttemptId: @NotNull
- status: @NotNull
- amount: @NotNull @Positive

Domain validation must still exist.
API validation does not replace domain invariants.

==================================================
16. TESTING REQUIREMENTS
==================================================

The assignment minimum:
- At least 1 unit test validating domain state transition

Optional but strong:
- Integration test covering happy path
- Test duplicate webhook

Implement more than minimum if possible.

Required tests:
1. Unit test for Order state machine:
   - CREATED -> PENDING_PAYMENT
   - PENDING_PAYMENT -> PAID
   - PENDING_PAYMENT -> PAYMENT_FAILED
   - PAYMENT_FAILED -> PENDING_PAYMENT
   - PAID cannot change

2. Integration test happy path:
   - create cart
   - add item
   - checkout
   - start payment
   - webhook CONFIRMED
   - order becomes PAID

3. Integration test payment failure + retry:
   - start payment
   - webhook FAILED
   - order PAYMENT_FAILED
   - start payment again
   - new PaymentAttempt created
   - webhook CONFIRMED
   - order PAID

4. Duplicate webhook test:
   - same providerEventId arrives twice
   - first changes state
   - duplicate returns success acknowledgement
   - no duplicate transition

5. Edge cases:
   - cannot checkout empty cart
   - cannot modify checked-out cart
   - duplicate checkout returns existing order
   - duplicate payment start returns existing active attempt
   - amount mismatch webhook rejected and does not change state
   - conflicting late webhook ignored
   - PAID cannot be reverted

==================================================
17. README REQUIREMENTS
==================================================

Create a clear README.md covering:

- How to run the app
- H2 console details if enabled
- API list
- Sample requests
- Business assumptions
- Key design decisions
- Architecture overview
- Package/module structure
- Order state machine
- Payment flow
- Webhook idempotency
- Duplicate handling
- Failure/retry handling
- Trade-offs
- Future improvements:
  - refunds
  - cancellation
  - partial payments
  - PostgreSQL
  - outbox pattern
  - idempotent consumers
  - microservices extraction
  - eventual consistency

The technical review will focus on understanding, so the README must be easy to walk through.

==================================================
18. IMPLEMENTATION PLAN
==================================================

Implement in this order:

Step 1:
Project setup:
- pom.xml
- dependencies
- application.yml/properties
- H2 config
- SQL init config
- Lombok

Step 2:
Common module:
- Response
- GeneralResponse
- GenericResponse<T>
- ResultCode
- BusinessException
- NotFoundException
- ConflictException
- Validation error handling
- GlobalExceptionHandler

Step 3:
Cart module:
- CartStatus
- Cart
- CartItem
- repository
- request/response DTOs
- mapper
- CartApplicationService
- CartController

Step 4:
Order module:
- OrderStatus
- Order
- OrderItem
- repository
- DTOs
- mapper
- OrderQueryService
- OrderCommandService
- OrderController

Step 5:
Checkout:
- CheckoutApplicationService in cart.application
- Uses OrderCommandService
- No direct OrderRepository in cart module

Step 6:
Payment module:
- PaymentAttemptStatus
- WebhookProcessingResult
- PaymentAttempt
- PaymentWebhookEvent
- repositories
- DTOs
- mapper
- PaymentApplicationService
- WebhookApplicationService
- PaymentController

Step 7:
Mock provider:
- minimal controller/service to trigger CONFIRMED/FAILED webhook behavior

Step 8:
SQL:
- schema.sql
- data.sql if needed

Step 9:
Tests:
- domain unit tests
- integration tests

Step 10:
README

Step 11:
Final review and cleanup

==================================================
18.1 IMPLEMENTATION EXECUTION RULES
==================================================

Before generating code:
- Review all business invariants
- Review module ownership rules
- Review transaction boundaries
- Review idempotency requirements
- Review state transitions
- Review duplicate handling
- Review optimistic locking usage
- Review cross-module communication rules
- Validate architecture consistency before implementation

Priority order:
1. Correct business invariants
2. Correct state machine
3. Idempotency correctness
4. Clean architecture
5. Simplicity
6. Readability
7. Future extensibility
8. Fancy patterns last

Generate incrementally.

Phase 1:
- Project structure
- pom.xml
- common module
- shared response structure
- exceptions
- global exception handler

Wait for review before continuing.

Phase 2:
- Cart module

Wait for review before continuing.

Phase 3:
- Order module

Wait for review before continuing.

Phase 4:
- Payment module
- webhook processing
- mock provider

Wait for review before continuing.

Phase 5:
- tests
- README
- cleanup

Avoid:
- unnecessary interfaces
- generic base services
- abstract CRUD layers
- utility dumping
- god services
- anemic domain model
- unnecessary inheritance

Business rules belong inside domain entities whenever possible.

Application services orchestrate.
Entities enforce invariants.

All state transitions must be explicit and validated.

Never mutate statuses directly from external classes.

Use explicit names:
- CheckoutApplicationService
- PaymentApplicationService
- WebhookApplicationService
- OrderCommandService
- CartMapper
- PaymentMapper

Avoid vague names:
- Util
- Helper
- Manager
- Processor

The implementation will be reviewed in a technical architecture discussion.

The code must be easy to explain:
- why each module exists
- why each transaction exists
- why optimistic locking was used
- why idempotency is safe
- why PAID is final
- why retries create new PaymentAttempts

Do not start implementing immediately.

First:
1. Review the architecture
2. Review the domain model
3. Review the state machine
4. Review all business invariants
5. Review idempotency rules
6. Review duplicate handling rules
7. Review DB constraints
8. Review transaction boundaries
9. Review module ownership boundaries

Then explain the planned implementation structure briefly before generating code.

Always prefer:
- explicit code
- readable code
- maintainable code
- production-oriented code
- simple orchestration
- clear boundaries

Over:
- magic abstractions
- hidden framework behavior
- overengineering
- premature optimization

Keep the codebase easy for a Tech Lead review discussion.

The implementation should optimize for:
- correctness
- explainability
- maintainability
- safe payment handling
- state consistency
- idempotency safety

Future extensibility is important, but must not overcomplicate the first version.

Assume future extensions may include:
- refunds
- cancellations
- partial payments
- PostgreSQL migration
- microservices extraction
- event-driven communication

Design boundaries carefully to reduce future migration cost while keeping the current implementation simple.

Before finalizing:
- verify all flows manually
- verify duplicate scenarios
- verify retry scenarios
- verify invalid transition scenarios
- verify webhook idempotency
- verify optimistic locking usage
- verify no cross-module repository access
- verify PAID is immutable
- verify duplicate checkout safety
- verify duplicate payment start safety
- verify duplicate webhook safety

If any architectural conflict appears:
- prefer simplicity
- preserve business correctness
- preserve idempotency
- preserve state consistency
- avoid unnecessary patterns

Provide a short architecture review covering:
1. Module boundaries
2. Aggregate ownership
3. Transaction boundaries
4. State machine ownership
5. Idempotency strategy
6. Optimistic locking strategy
7. Duplicate handling strategy
8. Why the architecture is microservices-ready
9. Why this solution avoids overengineering
10. Potential future improvements

Then start implementation incrementally.

After each phase:
- explain what was implemented
- explain important decisions
- explain trade-offs
- explain assumptions
- explain business protections added
- explain idempotency protections added
- explain concurrency protections added
- explain intentional simplifications

Prefer constructor injection.
Avoid field injection.

Prefer LAZY loading by default unless eager loading is explicitly justified.

Be careful with equals/hashCode implementations on JPA entities.
Avoid Lombok-generated equals/hashCode on entities with relationships.

Use structured and meaningful logs.
Avoid excessive debug noise.

==================================================
19. STRICT DO / DO NOT RULES
==================================================

DO:
- Keep solution simple and explicit.
- Use rich domain behavior.
- Use DTOs.
- Use ResponseEntity.
- Use GenericResponse/GeneralResponse wrapper.
- Use BigDecimal.
- Use transactions at application service level.
- Use optimistic locking on Order.
- Use DB unique constraint for order.cartId.
- Use DB unique constraint for providerEventId.
- Add logs.
- Add tests.
- Add README.
- Keep modules isolated.
- Preserve future microservices readiness.

DO NOT:
- Do not expose entities from APIs.
- Do not put business logic in controllers.
- Do not access repositories across modules.
- Do not mutate Order status directly from outside Order domain.
- Do not reuse failed/confirmed payment attempts.
- Do not allow duplicate active payment attempts.
- Do not let duplicate webhooks change state twice.
- Do not let late webhook revert PAID order.
- Do not use double for money.
- Do not overengineer with distributed patterns.
- Do not introduce CANCELLED now.
- Do not use MapStruct.
- Do not use @Data on entities.

==================================================
20. FINAL ACCEPTANCE CHECKLIST
==================================================

Before finishing, verify:

- Application starts successfully.
- H2 schema loads successfully.
- All required endpoints exist.
- All responses use ResponseEntity + GenericResponse/GeneralResponse style.
- Cart checkout locks cart.
- Duplicate checkout does not create duplicate order.
- Order state machine enforces valid transitions.
- PAID is final.
- Payment start prevents duplicate active attempts.
- Failed payment can be retried with new PaymentAttempt.
- Confirmed webhook marks order PAID.
- Failed webhook marks order PAYMENT_FAILED.
- Duplicate webhook is idempotent.
- Invalid webhook does not corrupt business state.
- Amount mismatch webhook is rejected/recorded safely.
- Controllers are thin.
- @Transactional only on application services.
- No cross-module repository access.
- No entity exposed in API.
- Logs are present.
- Tests pass.
- README explains architecture, flows, decisions, and trade-offs.

Now implement the solution step by step.
After generating code, provide a short summary of:
1. What was implemented
2. Important files/classes
3. How to run
4. How to test
5. Any assumptions made