# Workspace Instructions for AI Agents

## Project Overview

**Smart Tourism Guide Backend** — A Spring Boot 3.2.5 REST API for a smart tourism guidance system (Dokuz Eylül University graduation thesis). Uses PostgreSQL+PostGIS for geospatial data, Redis for caching, MinIO for file storage, and JWT-based authentication.

## Build & Run Commands

```bash
# Install dependencies
mvn install

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Start infrastructure (PostgreSQL, Redis, MinIO)
docker-compose up -d
```

## Tech Stack

| Layer          | Technology                                    |
|----------------|-----------------------------------------------|
| Framework      | Spring Boot 3.2.5, Java 17, Jakarta EE        |
| Database       | PostgreSQL 15 + PostGIS (via Hibernate Spatial)|
| Migrations     | Liquibase (`src/main/resources/db/changelog/`) |
| Cache          | Redis 7                                        |
| Object Storage | MinIO                                          |
| Auth           | JWT (jjwt 0.12.5), stateless sessions          |
| Mapping        | MapStruct 1.5.5 + Lombok                       |
| Monitoring     | Spring Actuator + Prometheus (Micrometer)      |

## Architecture

**Feature-sliced layered architecture.** Each domain module is a top-level package under `com.tourguide.*`:

```
src/main/java/com/tourguide/
├── admin/           # Admin roles: superadmin, moderator, contenteditor
├── auth/            # JWT auth (JwtFilter, JwtUtil, AuthService)
├── badge/           # Gamification badge system
├── chat/            # Chat messages and sessions
├── common/          # Shared config, exceptions, entities, utilities
├── image/           # Image/file management (MinIO)
├── notification/    # Push notifications + device tokens
├── place/           # Places with geospatial support
├── quest/           # Quests, steps, verification
├── review/          # User reviews with moderation
├── route/           # Routes with ordered places
├── todo/            # User todo lists
├── user/            # User profiles, favorites
└── TourguideApplication.java
```

### Module Internal Structure

Each feature module follows this pattern:
- `*Controller.java` — REST endpoint (`@RestController`)
- `I*Service.java` — Service interface (prefixed with `I`)
- `*Service.java` — Service implementation (`@Service`)
- `*Repository.java` — Spring Data JPA repository
- `*Mapper.java` — MapStruct mapper (`@Mapper(componentModel = "spring")`)
- `dto/` — Request/Response DTOs
- Entity classes at module root

### Common Package (`com.tourguide.common`)

- `config/` — SecurityConfig, RedisConfig, MinioConfig, WebConfig, RateLimitFilter, etc.
- `exception/` — GlobalExceptionHandler + custom exceptions
- `entity/` — BaseEntity (shared superclass)
- `util/` — Utility classes

## Key Conventions

### Entities
- **Always extend `BaseEntity`** — provides `id` (UUID, auto-generated), `createdAt`, `updatedAt`, `isActive`
- Use `@Entity`, `@Table(name = "...")`, Lombok (`@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor`)
- Enums use `@Enumerated(EnumType.STRING)`
- Auditing via `@CreatedDate`, `@LastModifiedDate`

### DTOs
- Input DTOs: `*Request` (e.g., `CreatePlaceRequest`)
- Output DTOs: `*Response` (e.g., `PlaceResponse`)
- Validation with `@NotNull`, `@Size`, `@Valid`
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`

### Controllers
- `@RestController` + `@RequestMapping("/resource")`
- Inject service interfaces via `@RequiredArgsConstructor`
- Use `@AuthenticationPrincipal UUID userId` for authenticated user
- Return `ResponseEntity<DTO>` with appropriate HTTP status

### Services
- Implement an `I*Service` interface
- `@Slf4j` for logging, `@RequiredArgsConstructor` for DI
- `@Transactional(readOnly = true)` for reads, `@Transactional` for writes
- Throw custom exceptions: `ResourceNotFoundException`, `DuplicateResourceException`, `UnauthorizedException`
- Use MapStruct mappers for entity ↔ DTO conversion

### MapStruct Mappers
- `@Mapper(componentModel = "spring")`
- Interface-based with `toResponse(entity)` methods
- Use `@Mapping` for custom field transformations

### Error Handling
- Centralized in `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Unified `ErrorResponse` with: timestamp, status, message, path, validation details

### Security
- Roles: `SUPERADMIN`, `CONTENT_EDITOR`, `MODERATOR`, `TOURIST`
- Stateless sessions (`SessionCreationPolicy.STATELESS`)
- Public endpoints: `/auth/**`, `/actuator/health`
- Rate limiting filter applied before JWT filter

## Configuration Profiles

- `application.yml` — Base config
- `application-dev.yml` — Development (local Docker services)
- `application-prod.yml` — Production

## Infrastructure (Docker Compose)

| Service    | Image                  | Port(s)     |
|------------|------------------------|-------------|
| PostgreSQL | postgis/postgis:15-3.4 | 5432        |
| Redis      | redis:7-alpine         | 6379        |
| MinIO      | minio/minio:latest     | 9000, 9001  |

## Pitfalls & Notes

- **Lombok + MapStruct ordering**: Annotation processors must be configured in the correct order in `pom.xml` (Lombok → MapStruct → lombok-mapstruct-binding). Do not reorder.
- **Hibernate DDL is `none`**: All schema changes go through Liquibase changelogs, never `ddl-auto`.
- **UUID primary keys**: All entities use `UUID` with `GenerationType.UUID`. Do not use Long/Integer IDs.
- **PostGIS**: Place entities use spatial types (via `hibernate-spatial`). Ensure PostGIS extension is enabled in the database.
- **README is in Turkish**: The project README and comments may be in Turkish (academic thesis context).
