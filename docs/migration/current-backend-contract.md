# Current Backend Contract

## Runtime

- Backend port: `9933`
- Spring application name: `data-agent`
- Runtime language before migration: Kotlin on Java 21
- Target backend shell for this migration slice: Java 21 with Spring Boot
- Database: PostgreSQL
- Primary datasource config path: `spring.datasource`
- Vector store: Spring AI pgvector
- Graph runtime: Spring AI Alibaba Graph
- Public agent protocol: A2A JSON-RPC over HTTP/SSE

## Public Endpoints To Preserve

- `GET /.well-known/agent-card.json`
- `POST /a2a/jsonrpc`

## Generated Endpoints Currently Provided By Jimmer

- `GET /ts.zip`
- `GET /openapi`
- `GET /openapi-ui`

The migration replacement should expose OpenAPI through springdoc:

- `GET /v3/api-docs`
- `GET /openapi-ui`

## Database Tables To Preserve

- `db_table`
- `db_column`
- `db_foreign_key`
- `question_knowledge`
- `glossary_knowledge`
- `vector_store`

## Frontend Coupling

- `data-agent-frontend/scripts/generate-api.js` currently downloads `http://localhost:9933/ts.zip`.
- Generated frontend code is expected under `data-agent-frontend/src/apis/__generated`.
- The replacement must provide generated TypeScript models/services from OpenAPI in a later migration phase.
- Do not change frontend API generation in Phase 0 or Phase 1.

## Baseline Test Result

Command attempted from `data-agent-backend` on 2026-06-18:

```powershell
.\gradlew.bat test
```

Result: failed before Gradle execution with exit code `1`.

Key output:

```text
Could not find or load main class org.gradle.wrapper.GradleWrapperMain
java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

Observed cause: `data-agent-backend/gradle` contains no wrapper JAR files, so `gradlew.bat` cannot load `org.gradle.wrapper.GradleWrapperMain`.
