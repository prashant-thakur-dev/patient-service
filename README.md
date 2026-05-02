# Patient Service — Progress Log

## Project Overview

A **Spring Boot microservice** for managing patient records, part of a larger microservices architecture.

- **Tech Stack:** Java 21, Spring Boot 4.0.0, Spring Data JPA, PostgreSQL (prod), H2 (dev), Lombok, Bean Validation, springdoc-openapi 2.8.6
- **Base URL:** `/api/v1/patients`
- **Group ID / Artifact:** `com.pm` / `patient-service`

---

## What Has Been Done

### 1. Project Setup
- Spring Boot project initialized with Maven (`pom.xml`)
- Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`, `h2`, `lombok`, `spring-boot-devtools`, `springdoc-openapi-starter-webmvc-ui`

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
- **`PatientRequestDTO`** — incoming request body with Bean Validation (`@NotBlank`, `@Email`, `@Size`); `registerDate` is required only on create via `CreatePatientValidatorGroup`
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
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/patients` | Get all patients |
| `GET` | `/api/v1/patients/{id}` | Get single patient by ID |
| `POST` | `/api/v1/patients` | Create a new patient |
| `PUT` | `/api/v1/patients/{id}` | Update an existing patient |
| `DELETE` | `/api/v1/patients/{id}` | Delete a patient |

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
App in Docker: `http://localhost:8080/api/v1/patients`

---

## Current State

**Full CRUD + single-patient fetch implemented. Swagger UI available. Docker-ready.**

| Feature | Status |
|---|---|
| GET all patients | ✅ |
| GET patient by ID | ✅ |
| POST create patient | ✅ |
| PUT update patient | ✅ |
| DELETE patient | ✅ |
| Input sanitization (trim) | ✅ |
| registerDate in response | ✅ |
| 404 on PatientNotFound | ✅ |
| Multi-profile YAML config | ✅ |
| OpenAPI / Swagger UI | ✅ |
| Dockerfile + docker-compose | ✅ |

---

## What Can Be Done Next

### Architecture / Microservices
- [ ] **Service Discovery** — register with Eureka or Consul
- [ ] **API Gateway** — route requests through Spring Cloud Gateway
- [ ] **Inter-service communication** — add OpenFeign clients (e.g., appointment-service, billing-service)
- [ ] **Config Server** — externalize configuration via Spring Cloud Config

### Resilience & Observability
- [ ] **Actuator** — add `spring-boot-starter-actuator` for `/health`, `/info`, `/metrics`
- [ ] **Structured logging** — correlation IDs for tracing across services
- [ ] **Circuit Breaker** — Resilience4j for fault tolerance on inter-service calls
- [ ] **Distributed Tracing** — Micrometer + Zipkin/Jaeger

### API Improvements
- [ ] **Pagination & Sorting** — replace `findAll()` with `findAll(Pageable)` for large datasets
- [ ] **Swagger annotations** — add `@Operation`, `@ApiResponse`, `@Tag` to controller methods

### Security
- [ ] **Authentication** — secure endpoints with JWT / OAuth2 (Spring Security)
- [ ] **Authorization** — role-based access (e.g., ADMIN can delete, DOCTOR can read/update)
- [ ] **Externalize secrets** — move DB password out of `application-prod.yml` into env vars or a secrets manager

### Testing
- [ ] **Unit tests** — test `PatientService` with mocked repository
- [ ] **Integration tests** — test controller layer with `@SpringBootTest` + H2
- [ ] **Repository tests** — test custom queries with `@DataJpaTest`

---

## Git History Summary

| Commit | Description |
|---|---|
| `09d8e5c` | CRU Operation only D is remaining in patient service |
| *(session 1)* | Full CRUD complete; GET by ID; 404 fix; registerDate in response; input sanitization |
| *(session 2)* | Multi-profile YAML config; OpenAPI/Swagger; Docker support |
