# Seat Reservation at Scale

A concurrent seat reservation service built with Java 21, Spring Boot, PostgreSQL, Flyway, Micrometer, and Docker.

The service is designed around correctness under concurrent reservation attempts, idempotent requests, per-user booking limits, cancellation, observability, and database-backed consistency.

## Architecture

```text
Client
  |
  | HTTP/JSON
  v
Spring Boot API
  |
  +--------------------+
  |                    |
  v                    v
ReservationService   Actuator/Micrometer
  |
  v
PostgreSQL
  |
  +-- shows
  +-- seats
  +-- reservations
  +-- reservation_seats
  +-- user_show_bookings
```

PostgreSQL is the source of truth for seat ownership and reservation state.

## Technology

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Micrometer + Prometheus
- Maven
- Docker / Docker Compose
- JUnit / Spring Boot tests

## API

### Create a show

Admin-only endpoint.

```http
POST /shows
Authorization: Bearer <admin-token>
Content-Type: application/json
```

Example:

```json
{
  "name": "Mumbai Concert",
  "seats": ["A1", "A2", "A3", "A4", "A5"],
  "pricePaise": 25000
}
```

### Reserve seats

```http
POST /shows/{showId}/reserve
Authorization: Bearer <user-id>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

Example:

```json
{
  "seats": ["A1", "A2"]
}
```

Successful response:

```json
{
  "reservationId": "uuid",
  "showId": "uuid",
  "userId": "user-1",
  "seats": ["A1", "A2"],
  "amountPaise": 50000,
  "status": "CONFIRMED"
}
```

### Cancel a reservation

```http
POST /reservations/{reservationId}/cancel
Authorization: Bearer <user-id>
```

A cancelled reservation releases its seats back to `AVAILABLE`.

### Get show state

```http
GET /shows/{showId}
```

The response includes:

- total seats
- available seats
- held seats
- confirmed seats
- individual seat states

The invariant is:

```text
available + held + confirmed = total
```

### Health

```http
GET /actuator/health
```

### Readiness

```http
GET /actuator/health/readiness
```

### Prometheus metrics

```http
GET /actuator/prometheus
```

## Authentication

The assignment uses a lightweight bearer-token boundary.

For reservation and cancellation requests:

```text
Authorization: Bearer <user-id>
```

The bearer value becomes the authenticated user identity.

Show creation requires the configured admin token:

```text
Authorization: Bearer <admin-token>
```

For local development the default admin token is:

```text
admin-secret
```

Production deployments should provide the token through an environment variable rather than relying on the development default.

## Concurrency and correctness

The database is the authority for seat ownership.

Reservation attempts use an atomic conditional update equivalent to:

```sql
UPDATE seats
SET status = 'CONFIRMED'
WHERE id = ?
  AND status = 'AVAILABLE';
```

The reservation succeeds only when this update affects exactly one row.

If another concurrent transaction has already confirmed the seat, the update affects zero rows and the request returns:

```text
409 seat_taken
```

This prevents two concurrent users from successfully confirming the same seat.

For multi-seat reservations, the entire reservation operation runs inside one database transaction. If any requested seat cannot be confirmed, the transaction is rolled back so that a partial reservation is not created.

Seats are processed in deterministic order to reduce lock-order differences between concurrent multi-seat requests.

## Idempotency

Every reservation request requires an `Idempotency-Key`.

The key is scoped to:

```text
show + user + idempotency key
```

The request payload is also hashed.

Therefore:

```text
same key + same request
        |
        v
return original reservation
```

while:

```text
same key + different request
        |
        v
409 idempotency_conflict
```

This prevents duplicate reservations and duplicate effects when clients retry requests.

## Per-user booking limit

Each show has a default per-user limit of 4 seats.

A `user_show_bookings` row is locked while checking and updating the user's booked seat count.

This makes the limit safe under concurrent requests from the same user.

## Cancellation

Cancellation locks the reservation row and verifies ownership.

For an active reservation:

1. Reservation is locked.
2. Reservation ownership is verified.
3. Confirmed seats are changed back to `AVAILABLE`.
4. The user's booked-seat count is decremented.
5. Reservation status becomes `CANCELLED`.

All of these changes happen in one transaction.

## Holds

This implementation does not create temporary seat holds.

The model contains the `HELD` state because the API exposes the required seat-state vocabulary, but the current reservation flow uses:

```text
AVAILABLE -> CONFIRMED
AVAILABLE <- CONFIRMED
```

through reservation and cancellation.

A production hold implementation could add:

- hold creation
- hold expiry timestamps
- background expiration
- automatic release
- hold ownership

The explicit cancellation model was chosen for this implementation to keep the correctness boundary simple.

## Consistency vs availability

Seat reservation favors consistency over availability.

PostgreSQL is the authoritative source of seat ownership. If the database cannot be reached or cannot determine the current seat state, the service should fail the reservation rather than attempt an unsafe local decision.

This is intentional: accepting a reservation without authoritative seat state could result in double-selling.

## Observability

The service exposes Prometheus-compatible metrics including:

```text
seat_reservations_confirmed_total
seat_reservations_declined_total{reason="seat_taken"}
seat_reservations_declined_total{reason="per_user_limit"}
seat_reservations_idempotent_replays_total
available_seats
```

Requests also receive an `X-Correlation-Id` response header.

If a request does not provide one, the service generates a UUID.

The correlation ID is added to the logging MDC so that requests can be traced through application logs.

## Load testing

A hot-seat burst script is included in:

```text
scripts/burst-test.py
```

The script sends concurrent reservation attempts for the same seat and verifies:

- successful reservations
- declined reservations
- server errors
- decline reasons
- final seat state
- reconciliation invariant

### 1,000-request test

Result:

```text
201 confirmed: 1
409 declined: 999
5xx errors: 0
Other errors: 0

seat_taken: 999

available + held + confirmed = 100
```

### 5,000-request test

Result:

```text
201 confirmed: 1
409 declined: 4999
5xx errors: 0
Other errors: 0

seat_taken: 4999

available + held + confirmed = 100
```

### 20,000-request test

Result:

```text
201 confirmed: 1
409 declined: 19999
5xx errors: 0
Other errors: 0

seat_taken: 19999

available + held + confirmed = 100
```

The 20,000-request test used 300 concurrent workers.

### Concurrent idempotency test

A separate script:

```text
scripts/idempotency-test.py
```

sent 1,000 concurrent requests using the same user, seat, and idempotency key.

Result:

```text
201 responses: 1000
409 responses: 0
5xx responses: 0
Other responses: 0

Unique reservation IDs: 1

PASS: exactly-once idempotency holds under concurrency
```

## Running locally

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the application:

```bash
./mvnw spring-boot:run
```

The API is available at:

```text
http://localhost:8080
```

## Running with Docker

Build the application image:

```bash
docker build -t seat-reservation:latest .
```

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the application:

```bash
docker run --rm \
  --name seat-reservation-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5433/seat_reservation \
  -e SPRING_DATASOURCE_USERNAME=seat_app \
  -e SPRING_DATASOURCE_PASSWORD=seat_password \
  -e APP_ADMIN_TOKEN=admin-secret \
  seat-reservation:latest
```

Verify:

```bash
curl http://localhost:8080/actuator/health
```

## Tests

Run the automated tests:

```bash
./mvnw clean test
```

The current automated test suite passes successfully.

Concurrency behavior was additionally validated using the included burst scripts.

## Project structure

```text
src/main/java/com/suchet/seat_reservation
├── controller
├── dto
├── exception
├── filter
├── metrics
├── model
├── repository
├── security
└── service

src/main/resources
└── db/migration

scripts
├── burst-test.py
└── idempotency-test.py

Dockerfile
docker-compose.yaml
pom.xml
README.md
WRITEUP.md
```

## Future improvements

For a production deployment, the next improvements would include:

- Redis or another distributed rate-limiting layer
- asynchronous event publication
- payment integration with an outbox pattern
- explicit temporary holds and expiry
- distributed tracing
- database connection-pool tuning
- horizontal application scaling
- more extensive integration and failure-injection tests
- production secret management
- deployment behind a load balancer
- dashboards and alerting for Prometheus metrics
