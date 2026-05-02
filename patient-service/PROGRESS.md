# Patient Service — Progress Log

## Project Overview

A **Spring Boot microservice** for managing patient records, part of a larger microservices architecture.

- **Tech Stack:** Java 21, Spring Boot 4.0.0, Spring Data JPA, PostgreSQL (prod), H2 (dev), Lombok, Bean Validation, springdoc-openapi 2.8.6, Actuator
- **Base URL:** `/api/v1/patients`
- **Group ID / Artifact:** `com.pm` / `patient-service`
- **Default Port (local):** `4000` | **Docker port:** `8080`

---

## What Has Been Done

### 1. Project Setup
- Spring Boot project initialized with Maven (`pom.xml`)
- Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`, `h2`, `lombok`, `spring-boot-devtools`, `springdoc-openapi-starter-webmvc-ui`, `spring-boot-starter-actuator`

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
Three files added to project root:

- **`Dockerfile`** — two-stage build:
  - Stage 1 (`builder`): `maven:3.9.6-eclipse-temurin-21` → `mvn clean package -DskipTests`
  - Stage 2 (`runtime`): `eclipse-temurin:21-jre-alpine` → copies jar, exposes `8080`
- **`docker-compose.yml`** — two services:
  - `postgres` (`postgres:16-alpine`) with healthcheck via `pg_isready`
  - `patient-service` depends on postgres (`service_healthy`), overrides datasource URL/credentials via env vars
- **`.dockerignore`** — excludes `target/`, `.git/`, `*.md`, `.mvn/`, `mvnw`, `mvnw.cmd`

Run everything: `docker compose up --build`

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
- **`spring-boot-starter-actuator`** added to `pom.xml`
- Actuator endpoints exposed: `/actuator/health`, `/actuator/info`, `/actuator/metrics` (full health details always shown)
- `info.app` block in `application.yml`: name, version, description
- **`CorrelationIdFilter.java`** (`OncePerRequestFilter`):
  - Reads `X-Correlation-ID` request header (or generates a UUID if absent)
  - Puts correlation ID into MDC key `correlationId`
  - Echoes it back in `X-Correlation-ID` response header
  - Always calls `MDC.clear()` in `finally`
- Log pattern (both profiles): `%d{yyyy-MM-dd HH:mm:ss} [%thread] [correlationId=%X{correlationId}] %-5level %logger{36} - %msg%n`

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
| Dockerfile + docker-compose | ✅ |
| Unit tests (PatientService) | ✅ |
| Integration tests (Controller) | ✅ |
| Actuator endpoints | ✅ |
| Correlation ID filter + MDC logging | ✅ |

---

## API Endpoint Reference

> Local base URL: `http://localhost:4000`  
> Docker base URL: `http://localhost:8080`  
> Replace `<BASE_URL>` below with whichever applies.  
> Replace `<ID>` with a real patient UUID.

---

### GET all patients
```bash
curl -X GET <BASE_URL>/api/v1/patients \
  -H "Accept: application/json"
```

---

### GET patient by ID
```bash
curl -X GET <BASE_URL>/api/v1/patients/<ID> \
  -H "Accept: application/json"
```

---

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
**Response:** `201 Created`  
**Validation errors:** `400 Bad Request` (missing/invalid fields)  
**Duplicate email:** `400 Bad Request`

---

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
> Note: `registerDate` is NOT required on update.  

**Response:** `200 OK`  
**Not found:** `404 Not Found`  
**Duplicate email:** `400 Bad Request`

---

### DELETE patient
```bash
curl -X DELETE <BASE_URL>/api/v1/patients/<ID>
```
**Response:** `204 No Content`  
**Not found:** `404 Not Found`

---

### Swagger UI
```
http://localhost:4000/swagger-ui.html
```

### OpenAPI JSON
```bash
curl http://localhost:4000/api-docs
```

---

### Actuator endpoints
```bash
# Health check
curl http://localhost:4000/actuator/health

# App info
curl http://localhost:4000/actuator/info

# Metrics list
curl http://localhost:4000/actuator/metrics

# Specific metric (e.g. JVM memory)
curl http://localhost:4000/actuator/metrics/jvm.memory.used
```

---

### Using X-Correlation-ID header
Pass a correlation ID to trace a request across logs:
```bash
curl -X GET <BASE_URL>/api/v1/patients \
  -H "X-Correlation-ID: my-trace-id-001"
```
The same ID will be returned in the response header and printed in every log line for that request.

---

## What Can Be Done Next

### Architecture / Microservices
- [ ] **Service Discovery** — register with Eureka or Consul
- [ ] **API Gateway** — route requests through Spring Cloud Gateway
- [ ] **Inter-service communication** — add OpenFeign clients (e.g., appointment-service, billing-service)
- [ ] **Config Server** — externalize configuration via Spring Cloud Config

### Resilience & Observability
- [ ] **Circuit Breaker** — Resilience4j for fault tolerance on inter-service calls
- [ ] **Distributed Tracing** — Micrometer + Zipkin/Jaeger
- [ ] **Metrics scraping** — expose Prometheus-format metrics via Actuator + Micrometer

### API Improvements
- [ ] **Pagination & Sorting** — replace `findAll()` with `findAll(Pageable)` for large datasets
- [ ] **Swagger annotations** — add `@Operation`, `@ApiResponse`, `@Tag` to controller methods

### Security
- [ ] **Authentication** — secure endpoints with JWT / OAuth2 (Spring Security)
- [ ] **Authorization** — role-based access (e.g., ADMIN can delete, DOCTOR can read/update)
- [ ] **Externalize secrets** — move DB password out of `application-prod.yml` into env vars or a secrets manager

### Testing
- [ ] **Repository tests** — test custom queries with `@DataJpaTest`

---

## Git History Summary

| Commit | Description |
|---|---|
| `09d8e5c` | CRU Operation only D is remaining in patient service |
| *(session 1)* | Full CRUD; GET by ID; 404 fix; registerDate in response; input sanitization |
| *(session 2)* | Multi-profile YAML; OpenAPI/Swagger; Docker support |
| *(session 3)* | Unit tests; Integration tests; POST returns 201; Actuator; Correlation ID filter |