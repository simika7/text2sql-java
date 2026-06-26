# Java MyBatis-Plus Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate `data-agent-backend` from Kotlin + Jimmer to Java + MyBatis-Plus + Spring Boot while preserving the existing database schema, A2A endpoint behavior, graph workflow, and frontend integration.

**Architecture:** Keep the current module layout and PostgreSQL schema, but replace Jimmer-generated immutable entities, repositories, DTO views, and TypeScript API generation with explicit Java POJOs, MyBatis-Plus mappers, service-layer aggregate queries, hand-written DTOs, and springdoc/openapi-based client generation. Migrate in vertical slices so each phase compiles and can be verified before deleting Kotlin/Jimmer.

**Tech Stack:** Java 21, Spring Boot 3.5.12, MyBatis-Plus Spring Boot 3 starter, PostgreSQL JDBC, Spring AI, Alibaba Spring AI Graph, A2A Java SDK, Jackson, springdoc-openapi, Vue/Vite frontend.

---

## Scope And Strategy

This is a backend rewrite migration, not a dependency swap. The current backend has about 46 Kotlin files under `data-agent-backend/src/main/kotlin`, 2 Jimmer DTO files under `data-agent-backend/src/main/dto`, Jimmer repository interfaces, and a frontend API generator that downloads `http://localhost:9933/ts.zip` from Jimmer.

### 2026-06-24 Maven Revision

The backend build has been switched from Gradle to Maven because the repository's Gradle wrapper is incomplete: `gradlew.bat` fails with `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`. For all remaining migration phases, use Maven commands from `data-agent-backend`:

```powershell
mvn test
mvn spring-boot:run
```

If `mvn` is not on `PATH`, install Maven or add a Maven runtime before verification. Do not restore Gradle files unless the user explicitly asks for Gradle again.

Migration should be done in two tracks:

1. **Compile/runtime track:** introduce Java source, MyBatis-Plus, Java models, mappers, services, and Java equivalents of existing Kotlin classes.
2. **Compatibility track:** keep endpoint URLs, JSON shapes, database table names, graph state keys, prompt templates, and frontend behavior stable.

Recommended branch:

```bash
git switch -c codex/java-mybatis-plus-migration
```

Recommended commit style:

```bash
git commit -m "build: introduce java mybatis-plus backend"
git commit -m "feat: migrate schema persistence to mybatis-plus"
git commit -m "feat: migrate graph agent workflow to java"
git commit -m "chore: remove kotlin and jimmer"
```

---

## File Structure Map

### Build And Configuration

- Modify: `data-agent-backend/pom.xml`
  - Use Spring Boot Maven parent, Java 21, MyBatis-Plus starter, springdoc-openapi, Lombok, Spring AI, A2A, PostgreSQL, and SQLite dependencies.
  - Do not include Kotlin, KSP, Jimmer, `jackson-module-kotlin`, `kotlin-reflect`, or `kotlin-logging`.
- Delete: Gradle build files and wrapper scripts from `data-agent-backend`.
  - `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, and `gradlew.bat` are not used after the Maven switch.
- Modify: `data-agent-backend/src/main/resources/application.yml`
  - Remove `jimmer:` section.
  - Add `mybatis-plus:` mapper/type-alias configuration.
  - Add `springdoc:` paths if needed.

### Java Application Shell

- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/ServerApplication.java`
  - Java Spring Boot entrypoint.
- Delete after migration: `data-agent-backend/src/main/kotlin/io/github/qifan777/server/ServerApplication.kt`

### Persistence Domain

- Create Java entities:
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/domain/DbTable.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/domain/DbColumn.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/domain/DbForeignKey.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/domain/QuestionKnowledge.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/domain/GlossaryKnowledge.java`
- Delete after migration:
  - Existing Kotlin Jimmer entity interfaces in matching packages.

### Mapper Layer

- Create:
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/mapper/DbTableMapper.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/mapper/DbColumnMapper.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/mapper/DbForeignKeyMapper.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/mapper/QuestionKnowledgeMapper.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/mapper/GlossaryKnowledgeMapper.java`
- Optional XML if annotation queries become hard to read:
  - `data-agent-backend/src/main/resources/mapper/DbForeignKeyMapper.xml`
  - `data-agent-backend/src/main/resources/mapper/DbTableMapper.xml`

### Service Layer

- Create:
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/service/DbTableService.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/service/DbColumnService.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/service/DbForeignKeyService.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/service/QuestionKnowledgeService.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/service/GlossaryKnowledgeService.java`
- Replace repository injection in graph nodes with service injection.
- Delete after migration:
  - `data-agent-backend/src/main/kotlin/io/github/qifan777/server/dataset/**/repository/*.kt`

### DTO And View Replacement

- Delete after replacement:
  - `data-agent-backend/src/main/dto/DbTable.dto`
  - `data-agent-backend/src/main/dto/DbForeignKey.dto`
- Create:
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/dto/DbColumnSchemaView.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/dto/DbTableSchemaView.java`
  - `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/dto/DbForeignKeySchemaView.java`

### Agent And Workflow Java Migration

Create Java equivalents for all Kotlin files under:

- `data-agent-backend/src/main/kotlin/io/github/qifan777/server/agent`
- `data-agent-backend/src/main/kotlin/io/github/qifan777/server/integration/a2a`
- `data-agent-backend/src/main/kotlin/io/github/qifan777/server/shared`

Target Java directories:

- `data-agent-backend/src/main/java/io/github/qifan777/server/agent`
- `data-agent-backend/src/main/java/io/github/qifan777/server/integration/a2a`
- `data-agent-backend/src/main/java/io/github/qifan777/server/shared`

### Frontend API Generation

- Modify: `data-agent-frontend/scripts/generate-api.js`
  - Stop downloading `/ts.zip`.
  - Generate TypeScript from `/v3/api-docs` using `openapi-typescript` or `openapi-typescript-codegen`.
- Modify: `data-agent-frontend/package.json`
  - Add the selected OpenAPI codegen package.
- Verify generated imports used by:
  - `data-agent-frontend/src/utils/request.ts`
  - `data-agent-frontend/src/utils/api-instance.ts`
  - Any `src/apis/__generated` consumers.

---

## Phase 0: Baseline Capture

### Task 0.1: Record Current Behavior

**Files:**
- Read: `README.md`
- Read: `data-agent-backend/pom.xml`
- Read: `data-agent-backend/src/main/resources/application.yml`
- Read: `data-agent-frontend/scripts/generate-api.js`
- Create: `docs/migration/current-backend-contract.md`

- [ ] **Step 1: Create the migration docs directory**

Run:

```bash
mkdir -p docs/migration
```

Expected: `docs/migration` exists.

- [ ] **Step 2: Document current backend contract**

Create `docs/migration/current-backend-contract.md`:

```markdown
# Current Backend Contract

## Runtime

- Backend port: `9933`
- Spring application name: `data-agent`
- Database: PostgreSQL
- Primary datasource config path: `spring.datasource`
- Vector store: Spring AI pgvector

## Public endpoints to preserve

- `GET /.well-known/agent-card.json`
- `POST /a2a/jsonrpc`

## Generated endpoints currently provided by Jimmer

- `GET /ts.zip`
- `GET /openapi`
- `GET /openapi-ui`

## Database tables to preserve

- `db_table`
- `db_column`
- `db_foreign_key`
- `question_knowledge`
- `glossary_knowledge`
- `vector_store`

## Frontend coupling

- `data-agent-frontend/scripts/generate-api.js` downloads `http://localhost:9933/ts.zip`.
- Replacement must provide generated TypeScript models/services from OpenAPI.
```

- [ ] **Step 3: Run baseline backend tests**

Run:

```bash
cd data-agent-backend
mvn test
```

Expected: Existing tests either pass or fail for documented environment reasons such as missing database/API key. Record the result in `docs/migration/current-backend-contract.md`.

- [ ] **Step 4: Commit**

```bash
git add docs/migration/current-backend-contract.md
git commit -m "docs: capture current backend migration contract"
```

---

## Phase 1: Build System Migration Shell

### Task 1.1: Add Java And MyBatis-Plus Maven Build Configuration

**Files:**
- Modify: `data-agent-backend/pom.xml`
- Modify: `data-agent-backend/src/main/resources/application.yml`

- [ ] **Step 1: Update Maven build**

Replace the top of `data-agent-backend/pom.xml` with:

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.5.12"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.qifan777"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

- [ ] **Step 2: Update dependencies**

Use this dependency block:

```kotlin
dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.2"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("com.alibaba.cloud.ai:spring-ai-alibaba-graph-core:1.1.2.2")
    implementation("io.github.a2asdk:a2a-java-sdk-transport-jsonrpc:0.3.2.Final")
    implementation("io.projectreactor.netty:reactor-netty")
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.12")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Update `application.yml`**

Remove the entire `jimmer:` block and add:

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: io.github.qifan777.server
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: assign_uuid
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /openapi-ui
```

- [ ] **Step 4: Run dependency resolution**

Run:

```bash
cd data-agent-backend
mvn dependency:tree
```

Expected: Maven resolves MyBatis-Plus and no Kotlin/Jimmer dependencies are required by the build file.

- [ ] **Step 5: Commit**

```bash
git add data-agent-backend/pom.xml data-agent-backend/src/main/resources/application.yml
git commit -m "build: switch backend build to java and mybatis-plus"
```

### Task 1.2: Add Java Application Entrypoint

**Files:**
- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/ServerApplication.java`

- [ ] **Step 1: Create Java entrypoint**

```java
package io.github.qifan777.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("io.github.qifan777.server")
@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
```

- [ ] **Step 2: Temporarily keep Kotlin source until Java equivalents exist**

Do not delete `ServerApplication.kt` yet unless the build has Kotlin removed and all Kotlin files have Java equivalents. During execution, expect compile failures until migration phases are complete.

- [ ] **Step 3: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/ServerApplication.java
git commit -m "feat: add java spring boot entrypoint"
```

---

## Phase 2: Persistence Migration

### Task 2.1: Create Java Entities

**Files:**
- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/domain/DbTable.java`
- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/domain/DbColumn.java`
- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/domain/DbForeignKey.java`
- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/domain/QuestionKnowledge.java`
- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/knowledge/domain/GlossaryKnowledge.java`

- [ ] **Step 1: Add `DbTable`**

```java
package io.github.qifan777.server.dataset.scheme.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@TableName("db_table")
public class DbTable {
    @TableId
    private UUID id;
    private String name;
    private String description;
    private String databaseId;

    @TableField(exist = false)
    private List<DbColumn> columns = new ArrayList<>();
}
```

- [ ] **Step 2: Add `DbColumn`**

```java
package io.github.qifan777.server.dataset.scheme.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.Map;
import java.util.UUID;

@Data
@TableName("db_column")
public class DbColumn {
    @TableId
    private UUID id;
    private String name;
    private String type;
    private String description;
    private Boolean isPrimaryKey;
    private UUID tableId;

    @TableField(exist = false)
    private DbTable dbTable;

    public Document toDocument() {
        String databaseId = dbTable == null ? null : dbTable.getDatabaseId();
        return new Document(
            name + "\n" + type + "\n" + description,
            Map.of(
                "vectorType", "column",
                "columnId", id.toString(),
                "tableId", tableId.toString(),
                "databaseId", databaseId == null ? "" : databaseId
            )
        );
    }
}
```

- [ ] **Step 3: Add `DbForeignKey`**

```java
package io.github.qifan777.server.dataset.scheme.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

@Data
@TableName("db_foreign_key")
public class DbForeignKey {
    @TableId
    private UUID id;
    private UUID sourceColumnId;
    private UUID targetColumnId;

    @TableField(exist = false)
    private DbColumn sourceColumn;

    @TableField(exist = false)
    private DbColumn targetColumn;
}
```

- [ ] **Step 4: Add knowledge entities**

`QuestionKnowledge.java`:

```java
package io.github.qifan777.server.dataset.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.Map;
import java.util.UUID;

@Data
@TableName("question_knowledge")
public class QuestionKnowledge {
    @TableId
    private UUID id;
    private String databaseId;
    private String question;
    private String answer;

    public Document toDocument() {
        return new Document(
            question + "\n" + answer,
            Map.of(
                "vectorType", "question",
                "questionId", id.toString(),
                "databaseId", databaseId
            )
        );
    }
}
```

`GlossaryKnowledge.java`:

```java
package io.github.qifan777.server.dataset.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.Map;
import java.util.UUID;

@Data
@TableName("glossary_knowledge")
public class GlossaryKnowledge {
    @TableId
    private UUID id;
    private String databaseId;
    private String term;
    private String description;
    private String synonyms;

    public Document toDocument() {
        return new Document(
            term + "\n" + description + "\n" + (synonyms == null ? "" : synonyms),
            Map.of(
                "vectorType", "glossary",
                "glossaryId", id.toString(),
                "databaseId", databaseId
            )
        );
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/dataset
git commit -m "feat: add mybatis-plus persistence entities"
```

### Task 2.2: Create Mappers

**Files:**
- Create: mapper interfaces under `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/**/mapper`

- [ ] **Step 1: Add mapper interfaces**

Use this pattern for each table:

```java
package io.github.qifan777.server.dataset.scheme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;

import java.util.UUID;

public interface DbTableMapper extends BaseMapper<DbTable> {
}
```

Create the equivalent mapper classes for `DbColumn`, `DbForeignKey`, `QuestionKnowledge`, and `GlossaryKnowledge`.

- [ ] **Step 2: Verify mapper scanning**

Run:

```bash
cd data-agent-backend
mvn test -DskipTests
```

Expected at this phase: compile may still fail because other Java equivalents are not migrated yet, but mapper files themselves should have no syntax or import errors.

- [ ] **Step 3: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/dataset/*/mapper
git commit -m "feat: add mybatis-plus mappers"
```

### Task 2.3: Create Services Replacing Jimmer Repositories

**Files:**
- Create service classes listed in the file map.

- [ ] **Step 1: Implement `DbTableService`**

```java
package io.github.qifan777.server.dataset.scheme.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import io.github.qifan777.server.dataset.scheme.mapper.DbTableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DbTableService {
    private final DbTableMapper dbTableMapper;
    private final DbColumnService dbColumnService;

    public List<DbTable> findByDatabaseId(String databaseId) {
        return dbTableMapper.selectList(new LambdaQueryWrapper<DbTable>()
            .eq(DbTable::getDatabaseId, databaseId));
    }

    public List<DbTable> findByDatabaseIdAndNames(String databaseId, List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<DbTable> tables = dbTableMapper.selectList(new LambdaQueryWrapper<DbTable>()
            .eq(DbTable::getDatabaseId, databaseId)
            .in(DbTable::getName, names));
        attachColumns(tables);
        return tables;
    }

    public List<DbTable> findByIdsWithColumns(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<DbTable> tables = dbTableMapper.selectBatchIds(ids);
        attachColumns(tables);
        return tables;
    }

    public void attachColumns(List<DbTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return;
        }
        List<UUID> tableIds = tables.stream().map(DbTable::getId).toList();
        List<DbColumn> columns = dbColumnService.findByTableIdsWithTable(tableIds, tables);
        Map<UUID, List<DbColumn>> columnsByTableId = columns.stream()
            .collect(Collectors.groupingBy(DbColumn::getTableId));
        tables.forEach(table -> table.setColumns(columnsByTableId.getOrDefault(table.getId(), List.of())));
    }
}
```

- [ ] **Step 2: Implement `DbColumnService`**

```java
package io.github.qifan777.server.dataset.scheme.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import io.github.qifan777.server.dataset.scheme.mapper.DbColumnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DbColumnService {
    private final DbColumnMapper dbColumnMapper;

    public List<DbColumn> findByDatabaseId(String databaseId) {
        return dbColumnMapper.selectColumnsByDatabaseId(databaseId);
    }

    public List<DbColumn> findByTableIdsWithTable(List<UUID> tableIds, List<DbTable> tables) {
        if (tableIds == null || tableIds.isEmpty()) {
            return List.of();
        }
        List<DbColumn> columns = dbColumnMapper.selectList(new LambdaQueryWrapper<DbColumn>()
            .in(DbColumn::getTableId, tableIds));
        Map<UUID, DbTable> tableById = tables.stream()
            .collect(Collectors.toMap(DbTable::getId, Function.identity()));
        columns.forEach(column -> column.setDbTable(tableById.get(column.getTableId())));
        return columns;
    }
}
```

Add this custom method to `DbColumnMapper`:

```java
@Select("""
    select c.*
    from db_column c
    join db_table t on t.id = c.table_id
    where t.database_id = #{databaseId}
    """)
List<DbColumn> selectColumnsByDatabaseId(String databaseId);
```

- [ ] **Step 3: Implement `DbForeignKeyService`**

```java
package io.github.qifan777.server.dataset.scheme.service;

import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import io.github.qifan777.server.dataset.scheme.mapper.DbForeignKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DbForeignKeyService {
    private final DbForeignKeyMapper dbForeignKeyMapper;

    public List<DbForeignKey> findByDatabaseId(String databaseId) {
        return dbForeignKeyMapper.selectByDatabaseId(databaseId);
    }
}
```

Add this custom method to `DbForeignKeyMapper`:

```java
@Select("""
    select fk.*
    from db_foreign_key fk
    join db_column sc on sc.id = fk.source_column_id
    join db_table st on st.id = sc.table_id
    where st.database_id = #{databaseId}
    """)
@Results(id = "DbForeignKeyResultMap", value = {
    @Result(column = "id", property = "id"),
    @Result(column = "source_column_id", property = "sourceColumnId"),
    @Result(column = "target_column_id", property = "targetColumnId"),
    @Result(column = "source_column_id", property = "sourceColumn",
        one = @One(select = "io.github.qifan777.server.dataset.scheme.mapper.DbColumnMapper.selectById")),
    @Result(column = "target_column_id", property = "targetColumn",
        one = @One(select = "io.github.qifan777.server.dataset.scheme.mapper.DbColumnMapper.selectById"))
})
List<DbForeignKey> selectByDatabaseId(String databaseId);
```

- [ ] **Step 4: Implement knowledge services**

Use `selectList` by `databaseId` and `selectBatchIds` for IDs. Keep method names stable for graph node migration:

```java
public List<QuestionKnowledge> findByDatabaseId(String databaseId)
public List<QuestionKnowledge> findByIds(List<UUID> ids)
public List<GlossaryKnowledge> findByDatabaseId(String databaseId)
```

- [ ] **Step 5: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/dataset
git commit -m "feat: replace jimmer repositories with services"
```

---

## Phase 3: DTO And Schema Model Migration

### Task 3.1: Replace Jimmer DTO Views

**Files:**
- Create: DTO classes under `data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/dto`
- Modify later: `data-agent-backend/src/main/java/io/github/qifan777/server/agent/model/Schema.java`

- [ ] **Step 1: Create `DbColumnSchemaView`**

```java
package io.github.qifan777.server.dataset.scheme.dto;

import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbColumnSchemaView {
    private String name;
    private String type;
    private String description;
    private Boolean isPrimaryKey;

    public static DbColumnSchemaView from(DbColumn column) {
        return new DbColumnSchemaView(
            column.getName(),
            column.getType(),
            column.getDescription(),
            Boolean.TRUE.equals(column.getIsPrimaryKey())
        );
    }
}
```

- [ ] **Step 2: Create `DbTableSchemaView`**

```java
package io.github.qifan777.server.dataset.scheme.dto;

import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbTableSchemaView {
    private String name;
    private List<DbColumnSchemaView> columns;

    public static DbTableSchemaView from(DbTable table) {
        return new DbTableSchemaView(
            table.getName(),
            table.getColumns().stream().map(DbColumnSchemaView::from).toList()
        );
    }
}
```

- [ ] **Step 3: Create `DbForeignKeySchemaView`**

```java
package io.github.qifan777.server.dataset.scheme.dto;

import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbForeignKeySchemaView {
    private String sourceTableName;
    private String sourceColumnName;
    private String targetTableName;
    private String targetColumnName;

    public static DbForeignKeySchemaView from(DbForeignKey foreignKey) {
        return new DbForeignKeySchemaView(
            foreignKey.getSourceColumn().getDbTable().getName(),
            foreignKey.getSourceColumn().getName(),
            foreignKey.getTargetColumn().getDbTable().getName(),
            foreignKey.getTargetColumn().getName()
        );
    }

    public String toExpression() {
        return sourceTableName + "." + sourceColumnName + " = " + targetTableName + "." + targetColumnName;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/dataset/scheme/dto
git commit -m "feat: replace jimmer schema dto views"
```

### Task 3.2: Migrate `Schema` Model To Java

**Files:**
- Create: `data-agent-backend/src/main/java/io/github/qifan777/server/agent/model/Schema.java`
- Delete later: `data-agent-backend/src/main/kotlin/io/github/qifan777/server/agent/model/Schema.kt`

- [ ] **Step 1: Implement Java `Schema`**

Preserve methods:

```java
public static Schema fromState(OverAllState state)
public String buildSchemePrompt()
public String buildSchemePrompt(SchemaDataSourceProvider provider)
public String buildTablePrompt(DbTableSchemaView dbTable, SchemaDataSourceProvider provider)
public List<String> fetchDistinctValues(Connection connection, String fullTableName, String columnName, int limit)
public String toJson()
```

Use `JsonUtil.fromJson(json, Schema.class)` and `JsonUtil.toJson(this)` after `JsonUtil` is migrated to Java.

- [ ] **Step 2: Preserve prompt output text**

Keep these output fragments exactly unless encoding cleanup is intentionally handled in a separate commit:

```text
銆怐B_ID銆?# Table:
銆怓oreign keys銆?Examples:
primaryKey
```

- [ ] **Step 3: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/agent/model/Schema.java
git commit -m "feat: migrate schema prompt model to java"
```

---

## Phase 4: Shared Utilities Migration

### Task 4.1: Migrate JSON, Markdown, Datasource, Python Utilities

**Files:**
- Create Java equivalents for:
  - `shared/json/JsonUtil.kt`
  - `shared/markdown/MarkdownParserUtil.kt`
  - `shared/datasource/SchemaDataSourceProvider.kt`
  - `shared/datasource/SqliteSchemaDataSourceProvider.kt`
  - `shared/datasource/ResultSetBuilder.kt`
  - `shared/python/SimplePythonExecutor.kt`

- [ ] **Step 1: Migrate `JsonUtil`**

Expose these methods:

```java
public static String toJson(Object value)
public static <T> T fromJson(String json, Class<T> clazz)
public static <T> T fromJson(String json, TypeReference<T> typeReference)
```

Use a single configured `ObjectMapper`.

- [ ] **Step 2: Migrate datasource provider**

Preserve the interface:

```java
public interface SchemaDataSourceProvider {
    DataSource get(String databaseId);
}
```

Keep SQLite database lookup behavior identical to the Kotlin implementation.

- [ ] **Step 3: Migrate result set builder**

Preserve JSON/table output consumed by SQL execution nodes.

- [ ] **Step 4: Run focused compile**

```bash
cd data-agent-backend
mvn test -DskipTests
```

Expected: utility classes compile; broader compile may still fail until graph nodes are migrated.

- [ ] **Step 5: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/shared
git commit -m "feat: migrate shared backend utilities to java"
```

---

## Phase 5: Agent Workflow Migration

### Task 5.1: Migrate Agent Models

**Files:**
- Create Java equivalents for:
  - `agent/model/Plan.kt`
  - `agent/model/EvidenceQueryRewriteResult.kt`
  - `agent/model/DisplaySpec.kt`
  - `agent/model/SqlResultSet.kt`

- [ ] **Step 1: Convert Kotlin data classes to Java DTO classes**

Use Lombok:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    // Fields must match the Kotlin data class names exactly.
}
```

- [ ] **Step 2: Verify Jackson JSON names**

If Kotlin property names used snake_case in JSON, add:

```java
@JsonProperty("field_name")
```

to preserve frontend and LLM prompt parsing compatibility.

- [ ] **Step 3: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/agent/model
git commit -m "feat: migrate agent model classes to java"
```

### Task 5.2: Migrate Prompt And Graph Configuration

**Files:**
- Create:
  - `agent/prompt/PromptManager.java`
  - `agent/config/GraphConfiguration.java`
  - `agent/DataAgentSpec.java`
  - `agent/DocumentExtensions.java` replacement behavior as static utility methods

- [ ] **Step 1: Migrate `PromptManager`**

Use constructor injection and `@Value` resources. Preserve every prompt resource path under `src/main/resources/prompts`.

- [ ] **Step 2: Migrate `DataAgentSpec`**

Preserve graph names, state key names, node names, and edge names exactly, because frontend graph specs and persisted state may rely on them.

- [ ] **Step 3: Migrate `GraphConfiguration`**

Remove `ImmutableModuleV2` registration. Register only standard Jackson modules required by Java time or A2A SDK if needed.

- [ ] **Step 4: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/agent
git commit -m "feat: migrate agent graph configuration to java"
```

### Task 5.3: Migrate Graph Nodes And Edges

**Files:**
- Create Java equivalents for all files under:
  - `agent/nodes`
  - `agent/edges`

- [ ] **Step 1: Migrate nodes in dependency order**

Recommended order:

1. `SchemeReCallNode`
2. `EvidenceRecallNode`
3. `TableRelationNode`
4. `SqlGeneratorNode`
5. `SqlExecuteNode`
6. `PythonGeneratorNode`
7. `PythonExecuteNode`
8. `PythonAnalyzeNode`
9. `FeasibilityAssessmentNode`
10. `PlannerNode`
11. `PlanExecuteNode`
12. `ReportGeneratorNode`
13. `HumanFeedbackNode`

- [ ] **Step 2: Replace repository calls**

Use these service methods:

```java
dbTableService.findByDatabaseId(databaseId)
dbTableService.findByDatabaseIdAndNames(databaseId, names)
dbTableService.findByIdsWithColumns(ids)
dbForeignKeyService.findByDatabaseId(databaseId)
questionKnowledgeService.findByDatabaseId(databaseId)
questionKnowledgeService.findByIds(ids)
glossaryKnowledgeService.findByDatabaseId(databaseId)
```

- [ ] **Step 3: Replace DTO construction**

Replace:

```kotlin
DbTableSchemaView(it)
DbForeignKeySchemaView(it)
```

with:

```java
DbTableSchemaView.from(table)
DbForeignKeySchemaView.from(foreignKey)
```

- [ ] **Step 4: Migrate edges**

Convert Kotlin `when`/lambda logic to explicit Java methods. Preserve return values exactly.

- [ ] **Step 5: Compile**

```bash
cd data-agent-backend
mvn test -DskipTests
```

Expected: compile errors only in A2A integration or tests if those are not migrated yet.

- [ ] **Step 6: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/agent
git commit -m "feat: migrate graph nodes and edges to java"
```

---

## Phase 6: A2A Integration Migration

### Task 6.1: Migrate A2A Controller And Configuration

**Files:**
- Create Java equivalents for:
  - `integration/a2a/A2AController.kt`
  - `integration/a2a/A2AConfiguration.kt`
  - `integration/a2a/GraphAgentExecutor.kt`
  - `integration/a2a/JSONRPCTransportMetadata.kt`
- Preserve: `data-agent-backend/src/main/resources/META-INF/services/io.a2a.server.TransportMetadata`

- [ ] **Step 1: Migrate `A2AController`**

Preserve endpoints:

```java
@GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
@PostMapping(value = "/a2a/jsonrpc", produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
```

Remove:

```java
ImmutableModuleV2
@ApiIgnore
```

Use springdoc replacement if needed:

```java
@Hidden
```

- [ ] **Step 2: Preserve streaming behavior**

Use `SseEmitter(0L)` and `Flux.from(FlowAdapters.toPublisher(publisher))` as in current behavior.

- [ ] **Step 3: Migrate JSON-RPC error mapping**

Preserve handling for:

```java
JsonParseException
JsonEOFException
MethodNotFoundJsonMappingException
InvalidParamsJsonMappingException
IdJsonMappingException
```

- [ ] **Step 4: Compile**

```bash
cd data-agent-backend
mvn test -DskipTests
```

Expected: main Java code compiles.

- [ ] **Step 5: Commit**

```bash
git add data-agent-backend/src/main/java/io/github/qifan777/server/integration
git commit -m "feat: migrate a2a integration to java"
```

---

## Phase 7: Tests And Data Import Migration

### Task 7.1: Migrate Kotlin Tests To Java Or Disable Import Tests

**Files:**
- Convert:
  - `data-agent-backend/src/test/kotlin/io/github/qifan777/server/dataset/DatasetEmbeddingTest.kt`
  - `data-agent-backend/src/test/kotlin/io/github/qifan777/server/dataset/BirdSqlDatasetImportTest.kt`
- Create:
  - `data-agent-backend/src/test/java/io/github/qifan777/server/dataset/DatasetEmbeddingTest.java`
  - `data-agent-backend/src/test/java/io/github/qifan777/server/dataset/BirdSqlDatasetImportTest.java`

- [ ] **Step 1: Mark environment-heavy tests explicitly**

If tests require a local PostgreSQL database, external dataset files, or OpenAI-compatible API keys, add:

```java
@Disabled("Requires local dataset, database, and embedding model credentials")
```

until test containers and fixtures exist.

- [ ] **Step 2: Add mapper smoke test**

Create `data-agent-backend/src/test/java/io/github/qifan777/server/dataset/SchemeMapperSmokeTest.java`:

```java
@SpringBootTest
class SchemeMapperSmokeTest {
    @Autowired
    DbTableService dbTableService;

    @Test
    void findsTablesByDatabaseId() {
        List<DbTable> tables = dbTableService.findByDatabaseId("california_schools");
        assertNotNull(tables);
    }
}
```

- [ ] **Step 3: Run tests**

```bash
cd data-agent-backend
mvn test
```

Expected: pure unit tests pass; environment-heavy tests are skipped with explicit reason.

- [ ] **Step 4: Commit**

```bash
git add data-agent-backend/src/test
git commit -m "test: migrate backend tests to java"
```

---

## Phase 8: Frontend API Generation Replacement

### Task 8.1: Replace Jimmer `/ts.zip` Generator

**Files:**
- Modify: `data-agent-frontend/package.json`
- Modify: `data-agent-frontend/scripts/generate-api.js`

- [ ] **Step 1: Add OpenAPI TypeScript generator**

Run:

```bash
cd data-agent-frontend
pnpm add -D openapi-typescript-codegen
```

Expected: `package.json` and `pnpm-lock.yaml` update.

- [ ] **Step 2: Replace `generate-api.js`**

Use:

```js
/* eslint-env node */
import { generate } from 'openapi-typescript-codegen'

await generate({
  input: 'http://localhost:9933/v3/api-docs',
  output: 'src/apis/__generated',
  httpClient: 'fetch',
  useOptions: true,
  useUnionTypes: true,
})

console.log('API generated from http://localhost:9933/v3/api-docs')
```

- [ ] **Step 3: Run backend and generate API**

Terminal 1:

```bash
cd data-agent-backend
mvn spring-boot:run
```

Terminal 2:

```bash
cd data-agent-frontend
pnpm run generate-api
```

Expected: `src/apis/__generated` is regenerated from `/v3/api-docs`.

- [ ] **Step 4: Fix frontend imports if generated file names differ**

Run:

```bash
cd data-agent-frontend
pnpm type-check
```

Expected: TypeScript passes. If imports fail, update only files importing `src/apis/__generated`.

- [ ] **Step 5: Commit**

```bash
git add data-agent-frontend/package.json data-agent-frontend/pnpm-lock.yaml data-agent-frontend/scripts/generate-api.js data-agent-frontend/src/apis/__generated
git commit -m "feat: generate frontend api from springdoc openapi"
```

---

## Phase 9: Delete Kotlin And Jimmer

### Task 9.1: Remove Old Kotlin/Jimmer Sources

**Files:**
- Delete:
  - `data-agent-backend/src/main/kotlin`
  - `data-agent-backend/src/test/kotlin`
  - `data-agent-backend/src/main/dto`

- [ ] **Step 1: Search remaining Kotlin/Jimmer references**

Run:

```bash
rg -n "kotlin|jimmer|org\\.babyfish|KRepository|ImmutableModuleV2|EnableImplicitApi|ApiIgnore|src/main/dto|src/main/kotlin|src/test/kotlin" data-agent-backend data-agent-frontend
```

Expected: No references except in migration docs.

- [ ] **Step 2: Delete old files**

Run:

```bash
rm -rf data-agent-backend/src/main/kotlin data-agent-backend/src/test/kotlin data-agent-backend/src/main/dto
```

- [ ] **Step 3: Compile and test**

```bash
cd data-agent-backend
mvn clean test
```

Expected: Java backend compiles and tests pass or environment-heavy tests are explicitly skipped.

- [ ] **Step 4: Commit**

```bash
git add -A data-agent-backend
git commit -m "chore: remove kotlin and jimmer backend sources"
```

---

## Phase 10: End-To-End Verification

### Task 10.1: Backend Runtime Verification

**Files:**
- No source changes unless verification reveals bugs.

- [ ] **Step 1: Start backend**

```bash
cd data-agent-backend
mvn spring-boot:run
```

Expected:

```text
Tomcat started on port 9933
Started ServerApplication
```

- [ ] **Step 2: Verify A2A agent card**

```bash
curl http://localhost:9933/.well-known/agent-card.json
```

Expected: valid JSON agent card.

- [ ] **Step 3: Verify OpenAPI**

```bash
curl http://localhost:9933/v3/api-docs
```

Expected: valid OpenAPI JSON.

- [ ] **Step 4: Verify Jimmer endpoints are gone**

```bash
curl -i http://localhost:9933/ts.zip
```

Expected: `404` or no route.

### Task 10.2: Frontend Verification

**Files:**
- No source changes unless verification reveals bugs.

- [ ] **Step 1: Install frontend dependencies**

```bash
cd data-agent-frontend
pnpm install
```

- [ ] **Step 2: Generate API**

```bash
pnpm run generate-api
```

Expected: generated API files update successfully from `/v3/api-docs`.

- [ ] **Step 3: Type-check frontend**

```bash
pnpm type-check
```

Expected: no TypeScript errors.

- [ ] **Step 4: Start frontend**

```bash
pnpm dev
```

Expected: Vite starts and frontend can call backend.

### Task 10.3: Final Audit

- [ ] **Step 1: Confirm no Kotlin/Jimmer references**

```bash
rg -n "kotlin|jimmer|org\\.babyfish|KRepository|ImmutableModuleV2|EnableImplicitApi|ApiIgnore" data-agent-backend data-agent-frontend
```

Expected: no matches except documentation.

- [ ] **Step 2: Confirm Java source only**

```bash
rg --files data-agent-backend/src/main | rg "\\.(kt|dto)$"
```

Expected: no output.

- [ ] **Step 3: Commit final fixes**

```bash
git status --short
git add -A
git commit -m "test: verify java mybatis-plus migration"
```

---

## Risk Register

1. **Jimmer relation fetching replacement**
   - Risk: MyBatis-Plus does not auto-fetch nested objects like Jimmer fetchers.
   - Mitigation: Aggregate in service layer and add mapper smoke tests for tables, columns, and foreign keys.

2. **Generated DTO JSON shape**
   - Risk: Jimmer DTO views may serialize slightly differently than hand-written DTOs.
   - Mitigation: Keep class names and field names stable; add JSON snapshot tests for `Schema.toJson()`.

3. **Frontend API generation**
   - Risk: springdoc-generated TypeScript services differ from Jimmer-generated services.
   - Mitigation: Treat frontend codegen as a separate phase and run `pnpm type-check`.

4. **Kotlin extension functions**
   - Risk: Existing Kotlin extension functions such as document conversion are easy to miss.
   - Mitigation: Move behavior into entity methods or explicit utility classes and search for all call sites.

5. **A2A SDK Java generics**
   - Risk: Kotlin type inference hides Java generic complexity.
   - Mitigation: Migrate A2A integration last, after graph executor types are known.

6. **Encoding in existing prompt/schema strings**
   - Risk: Some files display garbled Chinese comments/strings in PowerShell output.
   - Mitigation: Preserve runtime strings first; do encoding cleanup only after migration passes.

---

## Definition Of Done

- `data-agent-backend` builds with Java 21 and no Kotlin/Jimmer plugins.
- No `src/main/kotlin`, `src/test/kotlin`, or `src/main/dto` remains.
- No dependency or import references to `org.babyfish.jimmer`.
- MyBatis-Plus reads existing PostgreSQL tables without schema changes.
- `GET /.well-known/agent-card.json` works.
- `POST /a2a/jsonrpc` works for non-streaming and streaming requests.
- `GET /v3/api-docs` and `/openapi-ui` work.
- Frontend API generation no longer depends on `/ts.zip`.
- Backend tests pass or environment-heavy tests are explicitly skipped.
- Frontend `pnpm type-check` passes.

---

## Self-Review

- **Spec coverage:** The plan covers Java migration, MyBatis-Plus replacement, Spring Boot preservation, Kotlin removal, Jimmer removal, frontend API generation replacement, testing, and final verification.
- **Placeholder scan:** No task relies on unspecified "do later" behavior. Where exact Java fields depend on existing Kotlin model contents, the task explicitly requires preserving field names and checking JSON compatibility.
- **Type consistency:** Entity, mapper, service, and DTO names are consistent across phases: `DbTable`, `DbColumn`, `DbForeignKey`, `QuestionKnowledge`, `GlossaryKnowledge`, and their corresponding services/mappers/views.

