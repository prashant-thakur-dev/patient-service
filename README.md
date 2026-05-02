# Patient Service — Progress Log

## Project Overview

A **Spring Boot microservice** for managing patient records, part of a larger microservices architecture.

- **Tech Stack:** Java 21, Spring Boot 4.0.0, Spring Data JPA, PostgreSQL (prod), H2 (dev), Lombok, Bean Validation, springdoc-openapi 2.8.6, Actuator, Spring Cloud 2025.0.0
- **Base URL (local):** `http://localhost:4000/api/v1/patients`
- **Base URL (via Gateway):** `http://localhost:4004/api/v1/patients`
- **Group ID / Artifact:** `com.pm` / `patient-service`

---

## Microservices Architecture Overview

```
Client
  │
  ▼
api-gateway        :4004   (Spring Cloud Gateway — routes + correlation ID)
  │
  ├──► patient-service   :8080 / :4000   (REST API + JPA + PostgreSQL)
  │
  └──► [future services]

eureka-server      :8761   (Netflix Eureka — service registry)
postgres           :5432   (PostgreSQL — patient data)
```

All services register with Eureka. Gateway routes via `lb://` load-balanced URIs.

---

## What Has Been Done

### 1. Project Setup
- Spring Boot project initialized with Maven (`pom.xml`)
- Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`, `h2`, `lombok`, `spring-boot-devtools`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-actuator`, `spring-cloud-starter-netflix-eureka-client`

### 2. Domain Model
**`Patient.java`** — JPA entity with fields:
| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | Auto-generated primary key |
| `name` | `String` | Not null |
| `email` | `String` | Not null, unique, validated |
| `address` | `String` | Not null |
| `dateOfBirth` | `LocalDate` | Not null |
| `registerDate` | `LocalDate` | Not null |

### 3. DTOs
- **`PatientRequestDTO`** — incoming request body with Bean Validation (`@NotBlank`, `@Email`, `@Size`); `registerDate` required only on create via `CreatePatientValidatorGroup`
- **`PatientResponseDTO`** — outgoing response: `id`, `name`, `email`, `address`, `dateOfBirth`, `registerDate` (all as `String`)
- **`CreatePatientValidatorGroup`** — marker interface for create-only validation

### 4. Mapper
**`PatientMapper`** — static utility class:
- `toDTO(Patient)` — maps all fields including `registerDate`
- `toModel(PatientRequestDTO)` — converts request DTO → entity

### 5. Repository
**`PatientRepository`** extends `JpaRepository<Patient, UUID>`:
- `existsByEmail(String email)` — duplicate email check on create
- `existsByEmailAndIdNot(String email, UUID id)` — duplicate email check on update (excludes self)

### 6. Service Layer
**`PatientService`** — business logic:
- `getPatients()` — fetch all patients
- `getPatient(UUID id)` — fetch single patient; throws `PatientNotFoundException` if not found
- `createPatient(dto)` — trims name/email/address, validates unique email, saves, returns DTO
- `updatePatient(id, dto)` — trims name/email/address, validates email uniqueness (excluding self), updates fields, saves
- `deletePatient(id)` — checks existence, deletes by id

### 7. REST Controller
**`PatientController`** at `/api/v1/patients`:
| Method | Endpoint | Status | Description |
|---|---|---|---|
| `GET` | `/api/v1/patients` | `200` | Get all patients |
| `GET` | `/api/v1/patients/{id}` | `200` | Get single patient by ID |
| `POST` | `/api/v1/patients` | `201` | Create a new patient |
| `PUT` | `/api/v1/patients/{id}` | `200` | Update an existing patient |
| `DELETE` | `/api/v1/patients/{id}` | `204` | Delete a patient |

### 8. Exception Handling
**`GlobalExceptionHandler`** (`@RestControllerAdvice`):
- `MethodArgumentNotValidException` → `400 Bad Request` with field-level error map
- `EmailAlreadyExistException` → `400 Bad Request` with message
- `PatientNotFoundException` → `404 Not Found` with message

Custom exceptions: `EmailAlreadyExistException`, `PatientNotFoundException`

### 9. Multi-Profile Configuration
Three YAML config files (replaced `application.properties`):

| File | Profile | Database | DDL |
|---|---|---|---|
| `application.yml` | base (all) | — | active profile: `dev` |
| `application-dev.yml` | `dev` | H2 in-memory | `create-drop` |
| `application-prod.yml` | `prod` | PostgreSQL `localhost:5432` | `update` |

Switch profile at runtime: `--spring.profiles.active=prod`

### 10. OpenAPI / Swagger
- `springdoc-openapi-starter-webmvc-ui:2.8.6` added to `pom.xml`
- `OpenApiConfig.java` — `@Configuration` bean with API title, version, description, contact
- Swagger UI: `http://localhost:4000/swagger-ui.html`
- Raw OpenAPI JSON: `http://localhost:4000/api-docs`

### 11. Docker Support
- **`patient-service/Dockerfile`** — two-stage build; `eclipse-temurin:21-jre-alpine`; exposes `8080`
- **`patient-service/.dockerignore`** — excludes `target/`, `.git/`, `*.md`, `.mvn/`, `mvnw`
- **`docker-compose.yml`** moved to microservice root (sibling of all service folders)

### 12. Unit Tests
**`PatientServiceTest.java`** — 10 tests using JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`):
- `getPatients` — list returned correctly
- `getPatient` — found / not found (404)
- `createPatient` — saved / duplicate email
- `updatePatient` — updated / not found / duplicate email
- `deletePatient` — deleted / not found

### 13. Integration Tests
**`PatientControllerIntegrationTest.java`** — 11 tests using `@SpringBootTest` + MockMvc + H2 (`@ActiveProfiles("dev")`), `@Transactional` rollback per test:
- Covers all 5 endpoints, happy paths and error cases

### 14. Actuator & Structured Logging
- **`spring-boot-starter-actuator`** added; endpoints: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
- `info.app` block in `application.yml`: name, version, description
- **`CorrelationIdFilter.java`** (`OncePerRequestFilter`):
  - Reads or generates `X-Correlation-ID`; puts into MDC key `correlationId`; echoes in response header; clears MDC in `finally`
- Log pattern (both profiles): `%d{yyyy-MM-dd HH:mm:ss} [%thread] [correlationId=%X{correlationId}] %-5level %logger{36} - %msg%n`

### 15. Eureka Service Discovery
**New project: `eureka-server/`**
- Spring Cloud 2025.0.0 BOM; `spring-cloud-starter-netflix-eureka-server`
- `@EnableEurekaServer` on `EurekaServerApplication.java`
- Runs on port `8761`; self-registration disabled; zero sync wait for fast startup
- `eureka-server/Dockerfile` — two-stage build; `apk add curl` (required for docker healthcheck); exposes `8761`

**patient-service changes:**
- Added Spring Cloud 2025.0.0 BOM to `<dependencyManagement>`
- Added `spring-cloud-starter-netflix-eureka-client` dependency
- `application.yml` — added `eureka.client.service-url.defaultZone` + `prefer-ip-address: true`
- In Docker: `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/`

### 16. API Gateway
**New project: `api-gateway/`**
- Spring Cloud 2025.0.0 BOM; `spring-cloud-starter-gateway` + `eureka-client` + `actuator`
- Plain `@SpringBootApplication` — gateway auto-configures via Spring Cloud
- Runs on port `4004`
- Routes: `lb://patientservice` on path `/api/v1/patients/**` (load-balanced via Eureka)
- Discovery locator enabled — services auto-discoverable by name
- **`CorrelationIdGatewayFilter.java`** (`GlobalFilter`, `Ordered.HIGHEST_PRECEDENCE`):
  - Reactive (WebFlux) — uses `ServerWebExchange` / `Mono`
  - Reads or generates `X-Correlation-ID`; mutates downstream request headers; appends to response
- `api-gateway/Dockerfile` — two-stage build; `apk add curl`; exposes `4004`

**Root `docker-compose.yml`** (at `microservice/` level — tracks all services):
| Service | Image / Build | Port | Depends On |
|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `5432` | — |
| `eureka-server` | `./eureka-server` | `8761` | — |
| `api-gateway` | `./api-gateway` | `4004` | `eureka-server` healthy |
| `patient-service` | `./patient-service` | `8080` | `postgres` + `eureka-server` healthy |

---

## Current State

| Feature | Status |
|---|---|
| GET all patients | ✅ |
| GET patient by ID | ✅ |
| POST create patient (201) | ✅ |
| PUT update patient | ✅ |
| DELETE patient | ✅ |
| Input sanitization (trim) | ✅ |
| registerDate in response | ✅ |
| 404 on PatientNotFound | ✅ |
| Multi-profile YAML config | ✅ |
| OpenAPI / Swagger UI | ✅ |
| Dockerfile (patient-service) | ✅ |
| Unit tests (PatientService) | ✅ |
| Integration tests (Controller) | ✅ |
| Actuator endpoints | ✅ |
| Correlation ID filter + MDC logging | ✅ |
| Eureka Server | ✅ |
| Eureka Client (patient-service) | ✅ |
| API Gateway with routing | ✅ |
| Gateway Correlation ID filter | ✅ |
| Docker Compose (all services) | ✅ |

---

## API Endpoint Reference

> **Local (direct):** `http://localhost:4000`
> **Via Gateway:** `http://localhost:4004`
> Replace `<BASE_URL>` with either. Replace `<ID>` with a real patient UUID.

---

### GET all patients
```bash
curl -X GET <BASE_URL>/api/v1/patients \
  -H "Accept: application/json"
```

### GET patient by ID
```bash
curl -X GET <BASE_URL>/api/v1/patients/<ID> \
  -H "Accept: application/json"
```

### POST create patient
```bash
curl -X POST <BASE_URL>/api/v1/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "address": "123 Main St, Springfield",
    "dateOfBirth": "1990-05-15",
    "registerDate": "2024-01-10"
  }'
```
`201 Created` | `400` on invalid/duplicate email

### PUT update patient
```bash
curl -X PUT <BASE_URL>/api/v1/patients/<ID> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john.doe@example.com",
    "address": "456 New Blvd, Capital City",
    "dateOfBirth": "1990-05-15"
  }'
```
`registerDate` not required on update. `200 OK` | `404` not found | `400` duplicate email

### DELETE patient
```bash
curl -X DELETE <BASE_URL>/api/v1/patients/<ID>
```
`204 No Content` | `404` not found

---

### Swagger UI
```
http://localhost:4000/swagger-ui.html
```

### OpenAPI JSON
```bash
curl http://localhost:4000/api-docs
```

### Eureka Dashboard
```
http://localhost:8761
```

### Actuator (patient-service)
```bash
curl http://localhost:4000/actuator/health
curl http://localhost:4000/actuator/info
curl http://localhost:4000/actuator/metrics
curl http://localhost:4000/actuator/metrics/jvm.memory.used
```

### Actuator (api-gateway)
```bash
curl http://localhost:4004/actuator/health
```

### X-Correlation-ID (end-to-end tracing)
```bash
# Gateway generates one automatically if not supplied
curl -X GET http://localhost:4004/api/v1/patients \
  -H "X-Correlation-ID: my-trace-id-001"
# Same ID flows through gateway → patient-service logs
```

### Run everything with Docker
```bash
# from D:\certificate\neeT\video_material\microservice\
docker compose up --build

# Eureka dashboard  → http://localhost:8761
# API Gateway       → http://localhost:4004/api/v1/patients
# patient-service   → http://localhost:8080/api/v1/patients  (direct)
```

---

## What Can Be Done Next

### Architecture / Microservices
- [ ] **Inter-service communication** — add OpenFeign clients to call other services (e.g., appointment-service, billing-service)
- [ ] **Config Server** — externalize configuration via Spring Cloud Config

### Resilience & Observability
- [ ] **Circuit Breaker** — Resilience4j on inter-service calls
- [ ] **Distributed Tracing** — Micrometer + Zipkin/Jaeger (end-to-end trace with correlation ID)
- [ ] **Metrics scraping** — Prometheus format via Actuator + Micrometer

### API Improvements
- [ ] **Pagination & Sorting** — replace `findAll()` with `findAll(Pageable)`
- [ ] **Swagger annotations** — add `@Operation`, `@ApiResponse`, `@Tag` to controller methods

### Security
- [ ] **Authentication** — JWT / OAuth2 via Spring Security
- [ ] **Authorization** — role-based access (ADMIN can delete, DOCTOR can read/update)
- [ ] **Externalize secrets** — move DB credentials out of yml into env vars or secrets manager

### Testing
- [ ] **Repository tests** — test custom queries with `@DataJpaTest`
- [ ] **Gateway filter tests** — test `CorrelationIdGatewayFilter` with `WebTestClient`

---

## Git History Summary

| Commit | Description |
|---|---|
| `09d8e5c` | CRU Operation only D is remaining in patient service |
| `85c3f6b` | Implemented D (delete) in patient service |
| `a9ff000` | Added Docker and unit test code |
| `625aab8` | Added API Gateway and Eureka Server |
| *(current)* | Updated PROGRESS.md — full architecture documented |
