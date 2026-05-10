# ExeQut Architecture Decisions

## Architecture Style

* Modular Monolith
* Package-by-feature
* Layered architecture inside each module
* Rich Domain Model
* Explicit application service orchestration

## Modules

* cart
* order
* payment
* common

## Package Structure

* api
* application
* domain
* persistence

## Core Principles

* Controllers must stay thin
* Business logic belongs to domain entities
* Transactions belong to application services only
* Never expose JPA entities directly
* Use DTOs for API contracts
* Protect idempotency
* Enforce valid state transitions
* Use optimistic locking
* Use BigDecimal for money handling
* Follow clean module boundaries
* No direct cross-module repository access

## Cart Rules

* Cart can be modified only before checkout
* Checkout locks the cart
* Duplicate product adds increase quantity

## Order Rules

States:

* CREATED
* PENDING_PAYMENT
* PAYMENT_FAILED
* PAID

Rules:

* PAID is final
* Invalid transitions are forbidden
* Checkout creates exactly one order per cart

## Payment Rules

* PaymentAttempt represents each payment try
* Only one active INITIATED attempt per order
* Retry creates a new attempt
* Completed attempts are immutable

## Webhook Rules

* Webhooks must be idempotent
* Duplicate providerEventId must be ignored safely
* First terminal result wins
* Conflicting late events are audit-only

## Technical Stack

* Java 17
* Spring Boot
* Spring Data JPA
* H2 Database
* Maven
* Lombok
* Validation API

## API Style

Use:

* GenericResponse<T>
* GeneralResponse
* ResponseEntity

## Project Goal

Production-oriented implementation that is:

* simple
* explicit
* maintainable
* extensible
* easy to explain in technical review