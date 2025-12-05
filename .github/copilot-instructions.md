# Casero Web - AI Coding Agent Instructions

## Project Overview
Spring Boot 3.5 monolith reimplementing an Android customer/debt management app. Serves both Thymeleaf SSR web UI and RESTful API (`/api/v1/**`) for future mobile clients. Chilean peso amounts stored as integers (no decimals).

## Architecture

### Dual Authentication System
- **Web UI**: PIN-based authentication with session cookies (8h default). Custom `PinAuthenticationFilter` + `PinAuthenticationProvider` handle login at `/login`.
- **REST API**: Stateless JWT authentication. Separate `JwtAuthenticationFilter` validates Bearer tokens. Two security chains via `@Order(1)` API and `@Order(2)` web.
- PIN security: `PinHasher` component generates salt/hash/fingerprint. Matches via constant-time comparison.
- Roles: `ADMIN` (full access + admin panel) and `NORMAL` (standard operations).

### Domain Model
Core entities: `Customer` (name, sector, address, debt), `Transaction` (date, amount, balance, type, itemCount), `Statistic` (aggregated metrics), `Sector`, `AppUser`, `AuditEvent`.

TransactionTypes: `SALE`, `PAYMENT`, `REFUND`, `DEBT_FORGIVENESS`, `INITIAL_BALANCE`, `FAULT_DISCOUNT`. Use `isDebtDecreaser()` to identify balance-reducing transactions.

### Layer Pattern
- **web/controller/**: Thymeleaf controllers (return view names)
- **api/controller/**: REST controllers (`@RestController`, `/api/v1/**`)
- **service/**: Business logic, all transactional operations via `@Transactional`
- **repository/**: Spring Data JPA interfaces
- **domain/**: JPA entities (Lombok `@Getter/@Setter`)
- **util/**: Helpers like `PinHasher`, `DateTimeUtil`, `CustomerScoreCalculator`

### PostgreSQL Specifics
- **Requires `unaccent` extension** (created in V1 migration)
- Customer search uses `translate(lower(...), 'áéíóúñ', 'aeioun')` for accent-insensitive queries (not `unaccent()` function)
- Timezone hardcoded to `America/Santiago` in Hibernate and HikariCP init-sql
- Native queries use `@Query(value = "...", nativeQuery = true)` with `Pageable` support

### Transaction Processing
`TransactionServiceImpl` is the heart of the system:
1. All transaction operations update `Customer.debt` field
2. Create matching `Statistic` records for date-based reporting
3. Deletion triggers full recalculation via `recalculateCustomerBalances(customerId)`
4. Always use `OffsetDateTime` for `createdAt` timestamps

## Development Workflow

### Running Locally
```bash
# Start PostgreSQL (Docker)
./start-postgre.sh

# Run app (default profile: local, port 8080)
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=qa
```

Default credentials: Admin PIN `1111`, Normal PIN `2222` (from V2 migration).

### Testing
E2E tests use Playwright (`e2e/` directory):
```bash
# Interactive menu to select tests
./tests.sh --menu

# Specific test
E2E_SPEC=e2e/customer-flow.spec.js ./tests.sh

# Headless mode
PLAYWRIGHT_HEADLESS=true ./tests.sh
```

Tests use `data-testid` attributes for selectors (e.g., `getByTestId('customer-search-input')`).

### Database Migrations
Flyway auto-runs on startup (`src/main/resources/db/migration/`). Create new migrations as `V{N}__description.sql` where N increments sequentially. Never modify existing versioned migrations.

### Versioning
Uses `standard-version` (conventional commits):
```bash
npm run release         # patch bump
npm run release:minor   # minor bump
npm run release:major   # major bump
```

Updates `pom.xml`, `package.json`, `CHANGELOG.md`, creates git tag. Use conventional commit format: `feat:`, `fix:`, `refactor:`, `docs:`.

## Code Conventions

### Service Layer
- Always annotate with `@Transactional` (write operations) or `@Transactional(readOnly = true)` (read operations)
- Services return DTOs, not entities, for API controllers
- Use constructor injection (`@RequiredArgsConstructor` from Lombok)

### API Design
- REST endpoints: `/api/v1/{resource}`
- DTO classes in `api/dto/` with `Request`/`Response` suffixes
- Paginated responses use `PageResponse<T>` wrapper
- OpenAPI/Swagger enabled in local/qa, disabled in prod (`application-prod.properties`)

### Frontend Integration
- Thymeleaf templates in `src/main/resources/templates/`
- Fragments in `fragments/` (navbar, head, messages)
- Static assets in `static/` (CSS, JS, icons)
- Customer search only populates on user input (no auto-load of all customers)

### Audit Logging
`AuditEventServiceImpl` logs user actions (customer CRUD, transactions, config changes). Payload stored as JSONB. Admin users and login page views are skipped.

## Profile Configuration
- **local**: Full logging, OpenAPI enabled, `localhost:5432` DB
- **qa**: Reduced logging, OpenAPI enabled, `/casero` context path
- **prod**: Minimal logging, OpenAPI disabled, `/casero` context path

Override via `application-{profile}.properties` or environment variables (e.g., `SPRING_PROFILES_ACTIVE=prod`).

## Common Pitfalls
- Don't use PostgreSQL `unaccent()` function—use `translate()` pattern instead
- Customer debt is always stored in pesos as `INTEGER` (no cents)
- When deleting transactions, must call `recalculateCustomerBalances()` to fix debt/statistics
- JWT secret must be set via `SECURITY_JWT_SECRET` env var in production
- Docker Compose uses two networks: `internal` (app↔db) and `proxy-net` (Traefik routing)
