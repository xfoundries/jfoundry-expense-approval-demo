# Team Expense Approval Demo

English | [中文](README_ZH.md)

This is a business-light demo with a complete architecture path. It validates whether an AI agent
can use the [domain-architecture-skills](https://github.com/xfoundries/domain-architecture-skills) plugin and optional [jfoundry](https://github.com/xfoundries/jfoundry) support to move from complete
requirements through domain modeling, architecture decisions, implementation, and automated
acceptance. The project deliberately avoids expanding organization structures, approval matrices,
and other complex business concerns. Its focus is to verify that DDD, Hexagonal Architecture,
CQRS, Outbox, Inbox, Kafka, distributed locking, and persistence can be combined correctly.

This project explicitly selects a local jfoundry build to validate the plugin's `using-jfoundry`
landing phase. **jfoundry is not required by the `domain-architecture` plugin.** Projects that do
not use jfoundry can still use the domain modeling and architecture guidance capabilities
independently.

## Project Structure

```text
jfoundry-expense-approval-demo/
├── integration-contracts/          # Versioned cross-process contracts; no shared domain model
├── expense-approval-service/       # Expense approval service using DDD and Hexagonal Architecture
├── payment-processor-simulator/    # External payment simulator with deliberately simple business logic
└── end-to-end-tests/               # Complete path through both applications and real middleware
```

The root project is a Maven aggregator. Regular builds include the first three modules.
`end-to-end-tests` is enabled only by the `e2e` profile so that ordinary module builds do not
unconditionally start the complete container topology.

## Architecture and Technology

- Java 21, Maven, Spring Boot 3.5.16, jfoundry 1.0.0-SNAPSHOT
- MyBatis-Plus, PostgreSQL 17, Flyway
- Kafka, Redis/Redisson, Testcontainers
- JUnit 5, ArchUnit, Awaitility
- The command side uses the `ExpenseClaim` aggregate and explicit Command/Handler/Dispatcher types
- The query side uses view-oriented MyBatis queries and a payment-status projection without
  restoring aggregates
- CQRS does not use Event Sourcing; command tables and query projections remain in the same expense
  database

The expense service keeps Hexagonal roles explicit while organizing the application core by
business capability:

```text
expenseapproval
├── domain                    aggregate, value objects, Repository, and domain policy
├── application
│   ├── claim/command         commands and handlers; port/in contains the primary command Port
│   ├── claim/query           service and view models; port/in and port/out contain boundary contracts
│   ├── approval              final approval; port/out contains the approved-amount Port
│   ├── payment               payment projection; port/out contains its persistence Port
│   └── identity              approval actor and role
├── web                       HTTP Primary Adapter
├── messaging                 Kafka Primary Adapter
└── infrastructure            persistence, query, lock, and messaging Secondary Adapters
```

The application core is capability-first, with `port.in` and `port.out` nested only where directional
contracts are needed. `UseCase`, `Port`, and `Adapter` are used here because this branch explicitly
selects Hexagonal Architecture; they are not universal DDD naming rules and are not carried into the
separately maintained Onion variant.

Each mechanism has a distinct responsibility:

- `ExpenseClaim` protects state transitions and approval rules for one expense claim.
- MyBatis-Plus optimistic locking protects concurrent modification of one aggregate.
- A Redis lock serializes the cross-aggregate monthly limit check by employee and month.
- Transactional Outbox commits business changes and pending messages in the same database
  transaction.
- Inbox handles Kafka's at-least-once duplicate delivery by `eventId + consumerName`.
- Kafka carries only versioned integration events, not internal domain events.
- Payment results belong to the query projection. They do not enter the `ExpenseClaim` aggregate,
  and payment failure does not revoke approval.

## Complete Message Path

```text
HTTP command
  -> ExpenseClaim aggregate
  -> Expense PostgreSQL + approval Outbox
  -> Kafka: expense-approval.events.v1
  -> Payment Inbox + payment-result Outbox
  -> Kafka: payment.events.v1
  -> Expense Inbox + claim_payment_status projection
  -> HTTP query returns PENDING / PAID / FAILED
```

The default business rules retain only the complexity required to validate the architecture. An
employee's monthly final-approval limit is `10,000.00 CNY`. A payment succeeds when its amount does
not exceed `8,000.00 CNY`; larger payments return a deterministic failure result.

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker; container tests start two PostgreSQL instances, Kafka, and Redis
- Local jfoundry source at `/Users/huangxiao/Workspace/mine/jfoundry`

Install the current local jfoundry build before the first build:

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn clean install -DskipTests
```

## Tests and Acceptance

Run the regular module tests:

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry-expense-approval-demo
mvn test
```

Run acceptance tests that include the complete cross-application path:

```bash
mvn clean verify -Pe2e
```

The `e2e` profile uses Testcontainers to start two independent PostgreSQL instances, Kafka, Redis,
and two Spring Boot application contexts. It verifies five real scenarios:

1. Final approval results in successful payment and a `PAID` projection.
2. An amount above the single-payment limit produces a `FAILED` projection.
3. Redelivery of the same approval event is processed only once by the payment Inbox.
4. When two claims for the same employee and month compete concurrently for the monthly limit, only
   one receives final approval.
5. Exceeding the monthly limit rolls back the aggregate state and does not write an approval Outbox
   record.

The demo does not repeat jfoundry's dispatcher retry, recovery, cleanup, and concurrent-claim tests.
It verifies that those framework capabilities compose correctly at real business boundaries.

## Local Run

Local execution expects the following services:

- Expense PostgreSQL: `localhost:5432/expense_approval`
- Payment PostgreSQL: `localhost:5432/payment_simulator`
- Kafka: `localhost:9092`
- Redis: `localhost:6379`

At startup, each application executes the Flyway migrations under its module's
`src/main/resources/db/migration/` directory. The following environment variables override the
default connections:

| Environment variable | Default |
|---|---|
| `EXPENSE_APPROVAL_DB_URL` | `jdbc:postgresql://localhost:5432/expense_approval` |
| `EXPENSE_APPROVAL_DB_USERNAME` / `EXPENSE_APPROVAL_DB_PASSWORD` | `expense_approval` |
| `PAYMENT_SIMULATOR_DB_URL` | `jdbc:postgresql://localhost:5432/payment_simulator` |
| `PAYMENT_SIMULATOR_DB_USERNAME` / `PAYMENT_SIMULATOR_DB_PASSWORD` | `payment_simulator` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `EXPENSE_APPROVAL_REDIS_HOST` / `EXPENSE_APPROVAL_REDIS_PORT` | `localhost` / `6379` |

The default configuration disables Kafka listeners and the Outbox dispatcher so that starting one
application does not produce useless background retries. Enable them explicitly for complete local
integration:

```bash
export JFOUNDRY_OUTBOX_DISPATCHER_MODE=scheduled
export EXPENSE_APPROVAL_KAFKA_LISTENER_ENABLED=true
export PAYMENT_SIMULATOR_KAFKA_LISTENER_ENABLED=true

mvn -pl payment-processor-simulator spring-boot:run
```

Start the expense service in another terminal:

```bash
mvn -pl expense-approval-service spring-boot:run
```

The expense service listens on port `8080` by default:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

The payment simulator is a Kafka-driven non-web application and does not expose an HTTP port.

## Business API

Every expense request uses two simplified identity headers:

```text
X-User-Id: employee-1
X-User-Role: EMPLOYEE
```

The available roles are `EMPLOYEE`, `MANAGER`, and `FINANCE`. The main workflow lets an employee
create a draft, maintain expense items, and submit the claim. Manager approval immediately grants
final approval to a low-value claim; a high-value claim continues to finance approval. Final
approval checks the monthly limit and starts payment asynchronously. Nobody can approve their own
claim.

Example request to create an expense claim:

```bash
curl --fail-with-body -i -X POST http://localhost:8080/api/claims \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: employee-1' \
  -H 'X-User-Role: EMPLOYEE' \
  -d '{"title":"Customer visit"}'
```

When claim details are queried, the payment status eventually converges from `PENDING` to `PAID`
or `FAILED`.

---

## Validation Conclusions and Capability Boundaries

### Overall Assessment

Within the selected Java 21, Spring Boot, MyBatis-Plus, PostgreSQL, Kafka, and Redis stack, this demo
proves that the `domain-architecture` plugin and optional jfoundry support can guide an AI agent from
business requirements, domain modeling, and a Hexagonal Architecture decision through domain
implementation, CQRS, Outbox, Inbox, distributed locking, persistence, and end-to-end acceptance.

This conclusion does not come from the scaffold structure. It comes from a real business path: HTTP
commands enter an aggregate; business data and Outbox records commit in one transaction; Kafka
delivers across processes; both consumers use Inbox for idempotency; payment results reach a query
projection; and Redis locking plus a database transaction protects the cross-aggregate monthly
limit.

### Validation Evidence

- jfoundry completed two 67-module test matrices on Java 21 and Java 25.
- Every plugin skill, the Codex plugin manifest, and the Claude marketplace metadata passed
  validation.
- The demo passed 133 automated tests, including all five container-based end-to-end scenarios.
- End-to-end coverage includes payment success, payment failure, duplicate delivery, concurrent
  monthly-limit enforcement, and over-limit rollback without an Outbox record.
- A separately started local environment also completed the entire path from HTTP approval to a
  `PAID` payment projection through real middleware.

### Feedback to jfoundry

Development of the demo exposed and corrected several framework gaps:

- Unified aggregate persistence tracking and optimistic-lock context let single-table and
  multi-table aggregates share the same lifecycle entry points while keeping dependent-table
  synchronization an explicit responsibility of the business adapter.
- Repository lifecycle entry points became proxyable by Spring CGLIB, and the framework now
  translates known persistence-access failures consistently.
- An explicit `OutboxTemplate` supports translating internal domain events into independent,
  versioned integration events at the application boundary.
- PostgreSQL Inbox idempotency and the auto-configuration order for distributed locks, Outbox, and
  Kafka senders were corrected.
- Non-web applications gained Jackson support, and default Outbox JSON no longer leaks Java type
  metadata.

These changes followed runtime-neutral contracts, runtime adapters, and business responsibility
boundaries. They did not retain parallel implementations to preserve incorrect behavior.

### Feedback to the Domain Architecture Plugin

The plugin guidance was extended with the following conclusions:

- New projects must decide their project shape, such as single-module or multi-module, instead of
  letting a scaffold decide implicitly.
- Hexagonal, Onion, and other architecture styles must come from a prior architecture decision;
  Hexagonal Architecture is not a default.
- jfoundry remains an optional framework landing and does not block runtime-neutral domain modeling
  or architecture guidance.
- An aggregate Repository is first a DDD contract. Expressing it as a Secondary Port in Hexagonal
  Architecture is reasonable but does not replace its domain identity with a mandatory category.
- Guidance now covers multi-table aggregate persistence, exception boundaries, Spring proxies,
  portable integration-event JSON, broker sender validation, Outbox/Inbox adoption conditions, and
  contract translation responsibilities.
- Package-level architecture annotations apply only when every type in the package has the same
  role. Mixed packages should be split or use type-level annotations.

### Corrections in the Demo

The demo evolved from a single-module expense approval project into a four-module project while
keeping business logic deliberately simple. CQRS did not become a generic command bus, and the
payment simulator was not forced into a DDD aggregate. Every mechanism has one real and necessary
responsibility.

The project also corrected package-level Port annotations on mixed packages, the absence of an
independent Maven profile for E2E tests, and fixed sleeps. Kafka listeners and the Outbox dispatcher
are now disabled for ordinary local startup and explicitly enabled in the complete integration
environment. The application core is now grouped by claim, approval, payment, and identity rather
than by global `command`, `service`, `integration`, and `port` buckets; direction-specific Port
packages now live below their owning capabilities. Domain policy moved into the domain, while
Hexagonal Primary/Secondary Port and Adapter roles remain explicit at type level.

### Capability Boundaries

The current evidence confirms that, within the validated stack and business complexity, jfoundry
and the `domain-architecture` plugin are sufficient for an AI agent to develop a complete business
project using DDD, either of the separately validated Hexagonal or Onion architecture styles, and
reliable messaging. The styles remain separate architecture choices rather than one combined model.

This conclusion cannot be generalized directly to non-Spring runtimes, other ORMs, or other message
brokers. Security, observability, deployment, capacity, and production operations are also outside
this demo's validation scope. The Spring 7 composed-annotation
attribute-mapping issue is tracked by the open jMolecules
[issue #153](https://github.com/xmolecules/jmolecules/issues/153) and should not be worked around by
adding Spring `@AliasFor` to jfoundry's runtime-neutral modules. The BeanPostProcessor early
initialization issue exposed by the demo came from jfoundry Spring AOP auto-configuration rather
than `jmolecules-jackson`; jfoundry now uses Spring's canonical auto-proxy creator and lazily
resolves advisor interceptors. Further validation should prioritize a genuinely different runtime
or infrastructure implementation instead of adding more expense-approval business complexity.
