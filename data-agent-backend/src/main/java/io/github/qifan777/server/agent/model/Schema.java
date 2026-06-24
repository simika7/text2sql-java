package io.github.qifan777.server.agent.model;

import com.alibaba.cloud.ai.graph.OverAllState;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.dataset.scheme.dto.DbColumnSchemaView;
import io.github.qifan777.server.dataset.scheme.dto.DbForeignKeySchemaView;
import io.github.qifan777.server.dataset.scheme.dto.DbTableSchemaView;
import io.github.qifan777.server.shared.datasource.SchemaDataSourceProvider;
import io.github.qifan777.server.shared.datasource.SqliteSchemaDataSourceProvider;
import io.github.qifan777.server.shared.json.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schema {

    private static final Logger log = LoggerFactory.getLogger(Schema.class);

    private String databaseId;

    private List<DbTableSchemaView> dbTables;

    private List<DbForeignKeySchemaView> dbForeignKeys;

    private boolean enableExampleSampling = false;

    public static Schema fromState(OverAllState state) {
        String json = state.value(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, "");
        Schema schema = JsonUtil.fromJson(json, Schema.class);
        if (schema == null) {
            throw new RuntimeException("schema json deserialization failed");
        }
        return schema;
    }

    public String buildSchemePrompt() {
        return buildSchemePrompt(SqliteSchemaDataSourceProvider.INSTANCE);
    }

    public String buildSchemePrompt(SchemaDataSourceProvider dataSourceProvider) {
        StringBuilder schemeBuilder = new StringBuilder();
        schemeBuilder.append("銆怐B_ID銆?").append(databaseId).append('\n');
        for (DbTableSchemaView dbTable : nullToEmpty(dbTables)) {
            schemeBuilder.append(buildTablePrompt(dbTable, dataSourceProvider));
        }
        String keys = String.join("\n", nullToEmpty(dbForeignKeys).stream()
                .map(DbForeignKeySchemaView::toExpression)
                .toList());
        schemeBuilder.append("銆怓oreign keys銆慭n").append(keys);
        log.info("scheme prompt {}", schemeBuilder);
        return schemeBuilder.toString();
    }

    public String buildTablePrompt(DbTableSchemaView dbTable, SchemaDataSourceProvider dataSourceProvider) {
        StringBuilder builder = new StringBuilder();
        List<String> primaryKeys = nullToEmpty(dbTable.getColumns()).stream()
                .filter(column -> Boolean.TRUE.equals(column.getIsPrimaryKey()))
                .map(DbColumnSchemaView::getName)
                .toList();
        Map<String, List<String>> examplesByColumn = enableExampleSampling
                ? loadExamples(dbTable, dataSourceProvider)
                : Map.of();

        builder.append("# Table: ").append(dbTable.getName()).append("\n[\n");
        for (DbColumnSchemaView columnDto : nullToEmpty(dbTable.getColumns())) {
            builder.append("(")
                    .append(columnDto.getName())
                    .append(": ")
                    .append(columnDto.getType())
                    .append("\n, ")
                    .append(columnDto.getDescription())
                    .append(", ");
            if (primaryKeys.contains(columnDto.getName())) {
                builder.append("primaryKey, ");
            }
            List<String> examples = examplesByColumn.getOrDefault(columnDto.getName(), List.of());
            builder.append("Examples: [").append(String.join(", ", examples)).append("]),\n");
        }
        builder.append("]\n");
        return builder.toString();
    }

    private Map<String, List<String>> loadExamples(DbTableSchemaView dbTable, SchemaDataSourceProvider dataSourceProvider) {
        try (Connection connection = dataSourceProvider.get(databaseId).getConnection()) {
            Map<String, List<String>> examples = new LinkedHashMap<>();
            for (DbColumnSchemaView column : nullToEmpty(dbTable.getColumns())) {
                examples.put(column.getName(), fetchDistinctValues(connection, dbTable.getName(), column.getName(), 3));
            }
            return examples;
        } catch (Exception ex) {
            log.warn("load schema examples failed for table={}, databaseId={}, fallback to empty examples",
                    dbTable.getName(),
                    databaseId,
                    ex);
            return Map.of();
        }
    }

    public List<String> fetchDistinctValues(Connection connection, String fullTableName, String columnName, int limit) {
        List<String> values = new ArrayList<>();
        String sql = "SELECT DISTINCT `%s` FROM %s WHERE `%s` IS NOT NULL LIMIT %d"
                .formatted(columnName, fullTableName, columnName, limit);
        try (var stmt = connection.createStatement(); var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String value = rs.getString(1);
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        } catch (SQLException ignored) {
            // Some column types may not support DISTINCT; examples are optional.
        }
        return values;
    }

    public String toJson() {
        return JsonUtil.toJson(this);
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
