package io.github.qifan777.server.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.qifan777.server.agent.model.Schema;
import io.github.qifan777.server.agent.model.SqlResultSet;
import io.github.qifan777.server.dataset.knowledge.domain.QuestionKnowledge;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import io.github.qifan777.server.dataset.scheme.dto.DbColumnSchemaView;
import io.github.qifan777.server.dataset.scheme.dto.DbForeignKeySchemaView;
import io.github.qifan777.server.dataset.scheme.dto.DbTableSchemaView;
import io.github.qifan777.server.shared.datasource.ResultSetBuilder;
import io.github.qifan777.server.shared.json.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JavaMigrationSliceTest {

    @Test
    @SuppressWarnings("unchecked")
    void jsonUtilSupportsClassAndTypeReferenceRoundTrip() {
        String json = JsonUtil.toJson(Map.of("name", "orders", "count", 2));

        Map<String, Object> byClass = JsonUtil.fromJson(json, Map.class);
        Map<String, Object> byType = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
        });

        assertThat(byClass).containsEntry("name", "orders");
        assertThat(byType).containsEntry("count", 2);
        assertThat(JsonUtil.toJson(null)).isNull();
        assertThat(JsonUtil.fromJson(" ", Map.class)).isNull();
    }

    @Test
    void uuidOrNullParsesUuidValuesOnly() {
        UUID uuid = UUID.randomUUID();

        assertThat(DocumentExtensions.uuidOrNull(Map.of("id", uuid), "id")).isEqualTo(uuid);
        assertThat(DocumentExtensions.uuidOrNull(Map.of("id", uuid.toString()), "id")).isEqualTo(uuid);
        assertThat(DocumentExtensions.uuidOrNull(Map.of("id", "not-a-uuid"), "id")).isNull();
        assertThat(DocumentExtensions.uuidOrNull(Map.of(), "id")).isNull();
    }

    @Test
    void schemaBuildsPromptAndForeignKeyExpressions() {
        DbTableSchemaView orders = new DbTableSchemaView(
                "orders",
                List.of(
                        new DbColumnSchemaView("id", "INTEGER", "order id", true),
                        new DbColumnSchemaView("customer_id", "INTEGER", "customer id", false)
                )
        );
        DbForeignKeySchemaView foreignKey = new DbForeignKeySchemaView(
                new DbForeignKeySchemaView.ColumnRef(
                        "customer_id",
                        new DbForeignKeySchemaView.TableRef("orders")
                ),
                new DbForeignKeySchemaView.ColumnRef(
                        "id",
                        new DbForeignKeySchemaView.TableRef("customers")
                )
        );

        Schema schema = new Schema("db1", List.of(orders), List.of(foreignKey), false);

        assertThat(foreignKey.toExpression()).isEqualTo("orders.customer_id = customers.id");
        assertThat(schema.buildSchemePrompt())
                .startsWith("【DB_ID】db1\n")
                .contains("# Table: orders")
                .contains("(id: INTEGER")
                .contains("primaryKey")
                .contains("]\n【Foreign keys】\norders.customer_id = customers.id")
                .contains("orders.customer_id = customers.id");
        assertThat(schema.toJson()).contains("\"databaseId\":\"db1\"");
    }

    @Test
    void documentExtensionsPreserveQuestionKnowledgeConversion() {
        UUID id = UUID.randomUUID();
        QuestionKnowledge knowledge = new QuestionKnowledge(id, "db1", "How many orders?", "42");

        Document document = DocumentExtensions.toDocument(knowledge);

        assertThat(document.getText()).isEqualTo("How many orders?");
        assertThat(document.getMetadata())
                .containsEntry(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.QUESTION_KNOWLEDGE)
                .containsEntry(DataAgentSpec.Retrieval.DocumentMetadataKey.KNOWLEDGE_ID, id)
                .containsEntry(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, "db1")
                .doesNotContainKey("questionId");
    }

    @Test
    void documentExtensionsPreserveDbColumnConversion() {
        UUID tableId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        DbTable table = new DbTable(tableId, "orders", "orders table", "db1", List.of());
        DbColumn column = new DbColumn(columnId, "customer_id", "INTEGER", "customer id", false, UUID.randomUUID(), table);

        Document document = DocumentExtensions.toDocument(column);

        assertThat(document.getText()).isEqualTo("customer id");
        assertThat(document.getMetadata())
                .containsEntry(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.COLUMN)
                .containsEntry(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, "db1")
                .containsEntry(DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID, tableId)
                .containsEntry(DataAgentSpec.Retrieval.DocumentMetadataKey.COLUMN_ID, columnId);
    }

    @Test
    void resultSetBuilderCleansColumnNamesAndLimitsRows() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (id TEXT, name TEXT)");
            for (int i = 0; i < 1001; i++) {
                statement.execute("INSERT INTO sample (id, name) VALUES ('1', NULL)");
            }

            try (ResultSet resultSet = statement.executeQuery("SELECT id AS '`id`', name AS '\"name\"' FROM sample")) {
                SqlResultSet built = ResultSetBuilder.buildFrom(resultSet);

                assertThat(built.getColumn()).containsExactly("id", "name");
                assertThat(built.getData()).hasSize(1000);
                assertThat(built.getData().getFirst()).containsEntry("id", "1").containsEntry("name", "");
            }
        }
    }
}
