# Frame Inventory

## Overview
Frame Inventory is a take-home inventory management application for browsing, searching and filtering out-of-home advertising frames. It supports adding and editing frames, viewing frame change history and bulk importing inventory from CSV.

## Features

- Browse, search and filter frame inventory
- Add frames
- Edit frames
- View frame change history
- Bulk import frames from CSV
- Review partial CSV import results, including failed rows


## Architecture

```text
Browser
  ↓
React / TypeScript / Vite
  ↓ REST
Spring Boot / Java 21
  ↓ JPA
MariaDB
```
Docker Compose builds and runs the complete frontend, backend and database stack.


## Technology

| Module    | Stack                                     | Path            |
|-----------|-------------------------------------------|-----------------|
| backend   | Java 21, Spring Boot 3.5, Gradle, MariaDB | `src/backend/`  |
| frontend  | React 19, Vite, TypeScript                | `src/frontend/` |


## Running the application

The prerequisite is either Docker Desktop or Docker Engine with Docker Compose.

From the repository root, start the application with:

```bash
docker compose up
```

Compose builds missing images automatically. Use `docker compose up --build` when existing images need to be rebuilt after source changes.

| Service               | Host port | Container port | Notes                                                  |
|-----------------------|-----------|----------------|--------------------------------------------------------|
| frontend              | 3000      | 3000           | Vite dev server, proxies `/api` to the backend         |
| backend               | 8080      | 8080           | Spring Boot, exposes `GET /api/health`                 |
| mariadb-primary       | 3306      | 3306           | Intended for frame data                                |


### Credentials

| Service               | User       | Password       | Database         |
|-----------------------|------------|----------------|------------------|
| mariadb-primary       | `app`      | `app`          | `frames`         |

## Smoke test

Once the stack is up:

- Open <http://localhost:3000> and verify that the inventory screen loads.
- Verify backend health at <http://localhost:8080/actuator/health>.
- Verify the frontend proxy can reach the backend through <http://localhost:3000/api/health>.

Stop the stack with:

```bash
docker compose down 
```

`docker compose down` preserves the `mariadb-primary-data` volume and its database contents.

## Testing and Build

### Backend

Run backend tests:

```bash
cd src/backend
./gradlew test
```

Build the executable Spring Boot JAR:

```bash
./gradlew bootJar
```

### Frontend

Install dependencies:

```bash
cd src/frontend
npm ci
```

Run lint checks:

```bash
npm run lint
```

Build the frontend:

```bash
npm run build
```

There is currently no automated frontend test suite. Frontend behaviour was verified through build, lint, and end-to-end testing with the Docker Compose stack.


## Running modules without Docker

### Backend

The backend requires MariaDB to be running on `localhost:3306` with the `frames` database and configured credentials.

```bash
cd src/backend
./gradlew bootRun
```

Database connection can also be configured using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

### Frontend

The frontend expects the backend to be running on port `8080`.

```bash
cd src/frontend
npm install
npm run dev
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/frames` | Search and filter frames |
| GET | `/api/frames/{frameId}` | Get a frame |
| POST | `/api/frames` | Create a frame |
| PUT | `/api/frames/{frameId}` | Update a frame |
| GET | `/api/frames/{frameId}/history` | View frame history |
| POST | `/api/frames/import` | Import frames from CSV |



## Design Decisions

### Relational persistence and search

MariaDB and JPA are used for persistence and search. The supplied dataset contains approximately 1,000 frames, so relational search is sufficient for this exercise. OpenSearch was intentionally not introduced into the application path.

### Domain modelling

The CSV contains 45 fields. Related values are grouped into five embedded domain objects while remaining flattened in the `frames` table. This keeps the Java model readable without introducing extra tables or joins.

### CSV import

CSV import is create-only. Row-level validation or duplicate errors are isolated so valid rows can still be imported successfully. File-level validation errors, such as malformed CSV structure, missing required headers, invalid UTF-8, or inconsistent column counts, reject the entire file before any rows are persisted.

### Frame history

Frame creation, CSV import, and update operations are recorded in the `frame_history` table. `CREATED` and `IMPORTED` events store an empty `changedFields` object. `UPDATED` events are created only when at least one field actually changes, and they record the old and new values for those changed fields. A no-op update does not create a history event and does not change `modifiedDate`.

### Observability

The backend exposes Spring Boot Actuator health and metrics endpoints, includes request correlation using `X-Request-ID` and provides concise structured application logging. External monitoring infrastructure such as Prometheus, Grafana and distributed tracing is outside the scope of this exercise.


## Assumptions and Trade-offs

- **MariaDB/JPA search instead of OpenSearch:** Relational search keeps the application simpler and is sufficient for approximately 1,000 records. The accepted limitation is less capable advanced search and lower search scalability if the inventory grows substantially.
- **Embedded domain groups instead of normalized child tables:** Five embedded groups make the 45-field Java model easier to understand while avoiding joins for frame reads. The accepted cost is a wider `frames` table.
- **Per-row CSV transactions instead of one whole-file transaction:** Valid rows can still import when another row fails. The accepted consequence is partial-import behavior rather than all-or-nothing rollback.
- **Create-only CSV import instead of upsert:** Existing frames cannot be overwritten accidentally by an import. The accepted limitation is that CSV cannot bulk-update existing inventory.
- **JSON history instead of normalized field-change rows:** JSON provides a compact way to store field-level old and new values for frame updates. The accepted limitation is reduced SQL queryability of individual historical changes.
- **H2 persistence tests with MariaDB Docker verification:** H2 keeps automated persistence tests fast, while Docker verification exercises the application against the real MariaDB runtime. H2 compatibility mode cannot reproduce every MariaDB-specific behavior.

## Out of Scope
The following are intentionally not in scope for this take-home exercise:
- Authentication and authorization
- Production schema migration and provisioning
- Advanced search infrastructure

## AI-Assisted Development

OpenAI Codex and Entire were used during development to assist with code analysis, implementation, review and verification. Generated suggestions were reviewed before acceptance and changes were validated using automated tests, frontend build/lint checks and Docker end-to-end verification.


