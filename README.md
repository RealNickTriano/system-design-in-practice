# System Design in Practice

A hands-on study of building, deploying, and evolving a production-shaped web service. The
running example is a **Bitly-style URL shortener**, taken from a single Spring Boot app all the
way through containerization, infrastructure-as-code on AWS, load testing, and a planned split
into microservices.

The goal is less "ship a URL shortener" and more "work through the real engineering decisions" —
schema migrations, secret management, caching the hot read path, autoscaling, and service
decomposition — on a small but complete system.

## Repository layout

| Path | What it is |
|---|---|
| [`bitly/`](bitly/) | The baseline **monolith**: one Spring Boot service handling both URL creation and redirects, with full AWS infrastructure. |
| [`bitly-microservices/`](bitly-microservices/) | A copy of the monolith being evolved toward microservices. Currently the same code plus a Redis cache and a written [migration plan](bitly-microservices/MIGRATION_PLAN.md). The service split is designed but not yet implemented. |
| [`.github/workflows/`](.github/workflows/) | CI (test on every branch/PR) and Deploy (test → Terraform apply → build/push image → ECS rolling deploy on `main`). |

Both projects share the same package (`dev.nicktriano.bitly`) and the same architecture today;
they diverge as the migration plan gets carried out.

## What the application does

A URL shortener with two endpoints:

- **`POST /urls`** — shorten a URL. Accepts an optional custom `alias` and `expiration`.
  Returns the short URL and its expiration.
- **`GET /{shortCode}`** — resolve a short code and `302` redirect to the original URL.
  Returns `404` if unknown, `410 Gone` if expired.

### How short codes are generated

`UrlService` canonicalizes the URL (lowercases host, strips default ports and trailing slashes),
salts it with random bytes, SHA-256 hashes it, Base62-encodes the digest, and takes the first 8
characters. On a primary-key collision it retries up to 5 times before failing. Custom aliases
bypass generation and conflict with a `409` if already taken.

The redirect read path — the hot, latency-sensitive path — is cached in Redis via Spring's
`@Cacheable`, falling back to Postgres on a miss.

## Tech stack

- **Java 25**, **Spring Boot** (Web MVC, Data JPA, Validation, Actuator, Security), virtual threads enabled
- **PostgreSQL** with **Flyway** migrations (`hibernate.ddl-auto=validate` — schema is owned by migrations, not Hibernate)
- **Redis** for caching the redirect path
- **Docker** / Docker Compose for local Postgres + Redis
- **Terraform** for AWS infra: VPC, ALB (HTTPS via ACM), ECS Fargate, RDS, ECR, Route 53, Redis, Secrets Manager
- **GitHub Actions** for CI/CD with OIDC-based AWS auth (no long-lived keys)
- **k6** for load/stress testing the redirect path

## Infrastructure & deployment

Each project's [`infra/`](bitly/infra/) directory holds modular Terraform (`vpc`, `alb`, `acm`,
`dns`, `ecr`, `ecs`, `db`, `redis`). The GitHub Actions **Deploy** workflow runs on pushes to
`main`: it runs the test suite, applies Terraform, builds and pushes the Docker image to ECR, and
forces a new ECS deployment. AWS access uses GitHub OIDC (`github_oidc.tf`) rather than stored
credentials.

## Load testing

[`load_tests/shorten.js`](bitly/load_tests/shorten.js) is a k6 stress test that ramps the
redirect path from ~200 to ~1200+ req/s in plateaus to find the latency knee, while holding a
steady write load on `POST /urls`. Run it with:

```bash
k6 run load_tests/shorten.js
```

## The microservices migration

[`bitly-microservices/MIGRATION_PLAN.md`](bitly-microservices/MIGRATION_PLAN.md) documents the
plan to split the monolith into three services along their differing traffic profiles:

- **`gateway-service`** — Spring Cloud Gateway as the single entry point (routing, rate limiting, future auth)
- **`shorten-service`** — owns `POST /urls` (write path)
- **`redirect-service`** — owns `GET /{shortCode}` (read-heavy hot path, autoscaled, Redis-fronted)

It covers service decomposition, the strangler-fig approach to the shared database, ECS Service
Connect for service-to-service traffic, an RDS read replica for the redirect path, per-service
CI/CD, and the cutover order. The plan is written; the implementation is in progress.
