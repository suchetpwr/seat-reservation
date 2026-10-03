# Seat Reservation at Scale — Design Write-up

## 1. Overview

The goal of this service is to provide a seat reservation API that remains correct when many users attempt to reserve the same seat concurrently.

The primary design priorities were:

1. Prevent double-selling.
2. Make reservation requests idempotent.
3. Enforce a per-user booking limit.
4. Support cancellation and seat release.
5. Preserve an auditable reservation state.
6. Provide health, metrics, and request correlation.
7. Validate behavior under high concurrency.

The implementation uses Spring Boot and PostgreSQL, with PostgreSQL serving as the authoritative source of reservation state.

---

## 2. Atomic reservation decision

The central correctness requirement is that two users must never both successfully reserve the same seat.

The implementation does not rely on:

```text
read seat
check AVAILABLE
write CONFIRMED
```

as separate application-level decisions.

Instead, the seat transition itself is conditional:

```sql
UPDATE seats
SET status = 'CONFIRMED'
WHERE id = ?
  AND status = 'AVAILABLE';
```

The application checks the number of affected rows.

If the result is:

```text
1
```

the current transaction successfully claimed the seat.

If the result is:

```text
0
```

another transaction has already changed the state, so the request returns:

```text
409 seat_taken
```

This moves the critical race decision into the database, where it can be made atomically.

---

## 3. Multi-seat reservations

A request may contain multiple seats.

The reservation operation is transactional:

```text
BEGIN
  validate request
  check idempotency
  check user limit
  confirm each requested seat
  create reservation
  create reservation-seat mappings
  update user booking count
COMMIT
```

If any requested seat cannot be confirmed, the transaction rolls back.

Therefore, a request for:

```text
[A1, A2, A3]
```

cannot result in:

```text
A1 = CONFIRMED
A2 = CONFIRMED
A3 = AVAILABLE
```

with a successful reservation response.

The operation is all-or-nothing.

---

## 4. Deterministic seat ordering

Requested seats are sorted before the atomic updates are performed.

For example:

```text
[A3, A1, A2]
```

becomes:

```text
[A1, A2, A3]
```

This provides a deterministic ordering for multi-seat operations and reduces unnecessary lock-order differences between concurrent requests.

---

## 5. Idempotency

Clients may retry requests because of network failures, timeouts, or lost responses.

Without idempotency, the following sequence could create duplicate reservations:

```text
Client -> reserve A1
Server -> reservation created
Network response lost

Client -> retry reserve A1
Server -> another reservation
```

The service requires an `Idempotency-Key`.

The key is scoped by:

```text
show + user + idempotency key
```

The request payload is hashed and stored with the reservation.

Therefore:

```text
same key + same payload
```

returns the original reservation.

But:

```text
same key + different payload
```

returns:

```text
409 idempotency_conflict
```

This prevents the idempotency key from being reused for a materially different request.

---

## 6. Idempotency concurrency validation

The implementation was tested with 1,000 concurrent requests using:

```text
same user
same seat
same idempotency key
same request body
```

The result was:

```text
201 responses: 1000
409 responses: 0
5xx responses: 0
Other responses: 0

Unique reservation IDs: 1
```

Therefore, all concurrent retries resolved to the same reservation rather than creating multiple reservations.

---

## 7. Per-user limit

The default maximum number of seats a user can hold for a show is four.

A dedicated table maintains:

```text
show_id
user_id
booked_seats
```

The row is created if necessary and then locked before checking or updating the count.

The reservation transaction effectively performs:

```text
lock user/show booking row
check current count
check requested count
update count
```

This prevents concurrent requests from independently observing an outdated booking count and both exceeding the limit.

---

## 8. Cancellation

Cancellation is also transactional.

The service:

1. Locks the reservation.
2. Verifies that the requesting user owns it.
3. Verifies that it has not already been cancelled.
4. Releases its confirmed seats.
5. Decrements the user's booked-seat count.
6. Marks the reservation as `CANCELLED`.

The seat transition is:

```text
CONFIRMED -> AVAILABLE
```

This allows released seats to be reserved again.

Repeated cancellation is rejected rather than performing the release twice.

---

## 9. Reservation state

The reservation state is separate from the seat state.

Reservation:

```text
CONFIRMED
CANCELLED
```

Seat:

```text
AVAILABLE
HELD
CONFIRMED
```

The current implementation intentionally does not create temporary holds.

The `HELD` state remains represented in the domain model and show response, but the implemented flow uses explicit confirmation and cancellation.

A production implementation could introduce temporary holds with:

```text
hold_id
user_id
expires_at
```

and an expiration mechanism that atomically returns expired holds to `AVAILABLE`.

---

## 10. Consistency versus availability

The service chooses consistency over availability for reservation decisions.

PostgreSQL is the source of truth.

If the database is unavailable, the application should not attempt to make an independent seat-ownership decision.

The reason is straightforward:

```text
database unavailable
        +
local assumption that seat is available
        =
possible double sale
```

Failing a reservation during database unavailability is preferable to confirming a reservation that cannot be authoritatively persisted.

This design makes the reservation path dependent on the database's availability, but preserves the correctness invariant.

---

## 11. Reconciliation invariant

The show endpoint reports:

```text
availableSeats
heldSeats
confirmedSeats
totalSeats
```

The invariant is:

```text
availableSeats + heldSeats + confirmedSeats = totalSeats
```

This was validated repeatedly during concurrency tests.

The 20,000-request test ended with:

```text
available = 97
held = 0
confirmed = 3
total = 100
```

Therefore:

```text
97 + 0 + 3 = 100
```

The invariant held after the high-concurrency test.

---

## 12. High-concurrency testing

A hot-seat burst script was created to send many concurrent requests for the same seat.

### 1,000 requests

```text
1 confirmed
999 seat_taken
0 server errors
```

### 5,000 requests

```text
1 confirmed
4,999 seat_taken
0 server errors
```

### 20,000 requests

```text
1 confirmed
19,999 seat_taken
0 server errors
```

The 20,000-request test used 300 workers.

Across these tests, there were no observed double reservations and the reconciliation invariant remained valid.

---

## 13. Observability

The application exposes Prometheus metrics using Micrometer.

Implemented metrics include:

```text
seat_reservations_confirmed_total
seat_reservations_declined_total{reason="seat_taken"}
seat_reservations_declined_total{reason="per_user_limit"}
seat_reservations_idempotent_replays_total
available_seats
```

These provide visibility into:

- successful reservations
- seat contention
- per-user-limit violations
- idempotent retries
- current available inventory

The application also adds an `X-Correlation-Id` to requests.

If the client provides one, it is reused.

Otherwise, the application generates a UUID.

The value is placed into the logging MDC so application logs can be correlated with an individual request.

---

## 14. Health and readiness

Spring Boot Actuator provides:

```text
/actuator/health
/actuator/health/readiness
/actuator/prometheus
```

The readiness group includes the database health indicator.

This allows a deployment platform to distinguish application liveness from the application's ability to serve database-backed traffic.

---

## 15. Authentication

The implementation uses a lightweight bearer-token mechanism appropriate for the take-home assignment.

Reservation and cancellation requests use:

```text
Authorization: Bearer <user-id>
```

The bearer value identifies the user.

Show creation requires an admin token.

This is intentionally simpler than implementing a complete OAuth/OIDC system because authentication infrastructure is not the focus of the assignment.

In production, the authentication boundary would normally be delegated to an identity provider or API gateway.

---

## 16. Database schema

The main tables are:

### shows

Stores:

- show identity
- name
- price in paise
- per-user limit
- creation time

### seats

Stores:

- show
- seat number
- current seat status

### reservations

Stores:

- reservation identity
- show
- user
- amount
- status
- idempotency key
- request hash

### reservation_seats

Maps reservations to their seats.

### user_show_bookings

Maintains the number of currently booked seats per user and show.

Flyway manages schema migrations.

---

## 17. Monetary values

Prices are stored as integer paise rather than floating-point values.

For example:

```text
₹250.00 = 25000 paise
```

This avoids floating-point rounding issues in monetary calculations.

---

## 18. Docker

The application can be packaged as a Docker image.

The runtime architecture is:

```text
Docker
 |
 +-- Spring Boot application
 |
 +-- PostgreSQL
```

The application was successfully built into:

```text
seat-reservation:latest
```

and verified through the containerized health endpoint.

---

## 19. Testing strategy

The project contains automated Spring Boot tests and additional manual/concurrency test scripts.

Automated tests currently pass with:

```text
Tests run: 4
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The scripts provide additional validation of concurrency-specific behavior that is difficult to demonstrate with simple unit tests.

---

## 20. AI usage disclosure

AI assistance was used during development for implementation guidance, debugging, test design, troubleshooting, and documentation.

The resulting implementation was reviewed and tested manually, including API behavior, authentication boundaries, reservation correctness, idempotency, cancellation, metrics, Docker execution, and high-concurrency behavior.

The architectural decisions and final validation were reviewed against the assignment requirements.

---

## 21. Production improvements

If this service were taken beyond the take-home assignment, I would prioritize:

### Payment integration

Use an outbox/event-driven approach so reservation state and payment processing can be coordinated without losing events.

### Temporary holds

Add expiring holds for checkout flows where users need time to complete payment.

### Distributed scaling

Run multiple application instances behind a load balancer while keeping PostgreSQL as the authoritative reservation store.

### Database optimization

Tune:

- connection pools
- indexes
- transaction isolation
- lock contention
- query plans

based on production traffic.

### Rate limiting

Protect the API from abusive clients and accidental request storms.

### Distributed tracing

Add OpenTelemetry tracing across API, database, and downstream services.

### Failure testing

Introduce controlled database failures, application restarts, and network faults to validate recovery behavior.

### Production secrets

Move database credentials and admin credentials into a proper secret-management system.

---

## 22. Final design summary

The key correctness boundary is deliberately small:

```text
             reservation request
                     |
                     v
              validate request
                     |
                     v
              check idempotency
                     |
                     v
             lock user/show row
                     |
                     v
          atomically claim each seat
                     |
             +-------+-------+
             |               |
          success           failure
             |               |
             v               v
       create reservation   rollback
             |
             v
           commit
```

The database transaction is the authority for the reservation decision.

This provides:

- no double-selling
- atomic multi-seat reservations
- exactly-once idempotent retries
- per-user limits
- transactional cancellation
- auditable reservation state
- observable concurrency behavior
