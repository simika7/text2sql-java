package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.DocumentExtensions;
import io.github.qifan777.server.agent.model.Schema;
import io.github.qifan777.server.agent.prompt.PromptManager;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import io.github.qifan777.server.dataset.scheme.dto.DbForeignKeySchemaView;
import io.github.qifan777.server.dataset.scheme.dto.DbTableSchemaView;
import io.github.qifan777.server.dataset.scheme.service.DbForeignKeyService;
import io.github.qifan777.server.dataset.scheme.service.DbTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TableRelationNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(TableRelationNode.class);
    private static final double BASE_HIGH_SIMILARITY_THRESHOLD = 0.4;
    private static final int TARGET_TABLE_COUNT = 4;

    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;
    private final DbForeignKeyService dbForeignKeyService;
    private final DbTableService dbTableService;
    private final PromptManager promptManager;

    public TableRelationNode(
            ObjectMapper objectMapper,
            ChatModel chatModel,
            DbForeignKeyService dbForeignKeyService,
            DbTableService dbTableService,
            PromptManager promptManager
    ) {
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
        this.dbForeignKeyService = dbForeignKeyService;
        this.dbTableService = dbTableService;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        List<Document> tableDocuments = state.value(DataAgentSpec.Graph.StateKey.Recall.TABLE_SCHEMA, List.of());
        List<Document> columnDocuments = state.value(DataAgentSpec.Graph.StateKey.Recall.COLUMN_SCHEMA, List.of());
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");

        Map<UUID, Double> tableScores = tableDocuments.stream()
                .map(document -> {
                    UUID tableId = DocumentExtensions.uuidOrNull(
                            document.getMetadata(),
                            DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID
                    );
                    Double score = document.getScore();
                    return tableId == null || score == null ? null : Map.entry(tableId, score);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Math::max,
                        LinkedHashMap::new
                ));
        List<ColumnScore> columnScores = columnDocuments.stream()
                .map(document -> {
                    UUID tableId = DocumentExtensions.uuidOrNull(
                            document.getMetadata(),
                            DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID
                    );
                    UUID columnId = DocumentExtensions.uuidOrNull(
                            document.getMetadata(),
                            DataAgentSpec.Retrieval.DocumentMetadataKey.COLUMN_ID
                    );
                    Double score = document.getScore();
                    return tableId == null || columnId == null || score == null
                            ? null
                            : new ColumnScore(tableId, columnId, score);
                })
                .filter(Objects::nonNull)
                .toList();

        ThresholdSelection thresholdSelection = selectThreshold(tableScores, columnScores);
        List<UUID> tableIds = thresholdSelection.tableIds();
        Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable = thresholdSelection.highSimilarityColumnIdsByTable();
        List<DbForeignKey> foreignKeys = dbForeignKeyService.findByDatabaseId(databaseId);
        List<DbForeignKey> relatedForeignKeys = foreignKeys.stream()
                .filter(foreignKey -> {
                    UUID sourceTableId = tableIdOf(foreignKey.getSourceColumn());
                    UUID targetTableId = tableIdOf(foreignKey.getTargetColumn());
                    return tableIds.contains(sourceTableId) || tableIds.contains(targetTableId);
                })
                .toList();
        Set<UUID> foreignKeyRelatedTableIds = relatedForeignKeys.stream()
                .flatMap(foreignKey -> java.util.stream.Stream.of(
                        tableIdOf(foreignKey.getSourceColumn()),
                        tableIdOf(foreignKey.getTargetColumn())
                ))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<UUID> mergedTableIds = new LinkedHashSet<>();
        mergedTableIds.addAll(tableIds);
        mergedTableIds.addAll(foreignKeyRelatedTableIds);
        List<UUID> finalTableIds = new ArrayList<>(mergedTableIds);
        Map<UUID, Set<UUID>> foreignKeyColumnIdsByTable = relatedForeignKeys.stream()
                .flatMap(foreignKey -> java.util.stream.Stream.of(
                        foreignKey.getSourceColumn(),
                        foreignKey.getTargetColumn()
                ))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        column -> tableIdOf(column),
                        LinkedHashMap::new,
                        Collectors.mapping(DbColumn::getId, Collectors.toCollection(LinkedHashSet::new))
                ));

        List<DbTable> tables = dbTableService.findByIdsWithColumns(finalTableIds).stream()
                .map(table -> selectColumns(table, highSimilarityColumnIdsByTable, foreignKeyColumnIdsByTable))
                .toList();

        log.info(
                "table relation recall merged tables: direct={}, column={}, highTable={}, highColumn={}, threshold={}, fkExpanded={}, merged={}",
                tableScores.keySet().size(),
                thresholdSelection.columnTableScoreKeys(),
                thresholdSelection.highSimilarityTableCount(),
                thresholdSelection.highSimilarityColumnTableCount(),
                thresholdSelection.threshold(),
                foreignKeyRelatedTableIds.size(),
                finalTableIds.size()
        );

        String prompt = promptManager.mixSelectorPromptTemplate.render(Map.of(
                "schema_info", new Schema(
                        databaseId,
                        tables.stream().map(DbTableSchemaView::from).toList(),
                        relatedForeignKeys.stream().map(DbForeignKeySchemaView::from).toList(),
                        false
                ).buildSchemePrompt(),
                "question", rewriteQuery,
                "evidence", evidence
        ));
        log.info("mix select prompt {}", prompt);
        String filterResult = ChatClient.create(chatModel)
                .prompt()
                .options(noThinkingOptions())
                .user(prompt)
                .call()
                .content();
        if (filterResult == null) {
            throw new RuntimeException("mix select fail");
        }
        List<String> filterTableNames = objectMapper.readValue(filterResult, new TypeReference<>() {
        });
        List<DbTable> filterTables = dbTableService.findByDatabaseIdAndNames(databaseId, filterTableNames);
        List<DbForeignKey> filterForeignKeys = foreignKeys.stream()
                .filter(foreignKey -> filterTableNames.contains(tableNameOf(foreignKey.getTargetColumn()))
                        || filterTableNames.contains(tableNameOf(foreignKey.getSourceColumn())))
                .toList();
        log.info("mix select filter tables {}", filterResult);
        return Map.of(
                DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION,
                new Schema(
                        databaseId,
                        filterTables.stream().map(DbTableSchemaView::from).toList(),
                        filterForeignKeys.stream().map(DbForeignKeySchemaView::from).toList(),
                        false
                ).toJson()
        );
    }

    public record ColumnScore(UUID tableId, UUID columnId, double score) {
    }

    public record ThresholdSelection(
            double threshold,
            List<UUID> tableIds,
            Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable,
            int highSimilarityTableCount,
            int highSimilarityColumnTableCount,
            int columnTableScoreKeys
    ) {
    }

    public static ThresholdSelection selectThreshold(Map<UUID, Double> tableScores, List<ColumnScore> columnScores) {
        ThresholdSelection best = buildSelection(BASE_HIGH_SIMILARITY_THRESHOLD, tableScores, columnScores);
        if (best.tableIds().size() <= TARGET_TABLE_COUNT) {
            return best;
        }

        for (int i = 1; i <= 29; i++) {
            double threshold = Math.min(BASE_HIGH_SIMILARITY_THRESHOLD + i * 0.01, 0.99);
            ThresholdSelection candidate = buildSelection(threshold, tableScores, columnScores);
            int candidateDiff = Math.abs(TARGET_TABLE_COUNT - candidate.tableIds().size());
            int bestDiff = Math.abs(TARGET_TABLE_COUNT - best.tableIds().size());
            if (candidateDiff < bestDiff
                    || (candidateDiff == bestDiff && candidate.threshold() > best.threshold())) {
                best = candidate;
            }
        }
        return best;
    }

    private static ThresholdSelection buildSelection(
            double threshold,
            Map<UUID, Double> tableScores,
            List<ColumnScore> columnScores
    ) {
        Map<UUID, Double> tableScoreFromColumnRecall = columnScores.stream()
                .collect(Collectors.toMap(
                        ColumnScore::tableId,
                        ColumnScore::score,
                        Math::max,
                        LinkedHashMap::new
                ));
        Set<UUID> highSimilarityTableIds = tableScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ColumnScore> highSimilarityColumnScores = columnScores.stream()
                .filter(row -> row.score() >= threshold)
                .toList();
        Set<UUID> highSimilarityColumnTableIds = highSimilarityColumnScores.stream()
                .map(ColumnScore::tableId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable = highSimilarityColumnScores.stream()
                .collect(Collectors.groupingBy(
                        ColumnScore::tableId,
                        LinkedHashMap::new,
                        Collectors.mapping(ColumnScore::columnId, Collectors.toCollection(LinkedHashSet::new))
                ));
        LinkedHashSet<UUID> selectedTableIds = new LinkedHashSet<>();
        selectedTableIds.addAll(highSimilarityTableIds);
        selectedTableIds.addAll(highSimilarityColumnTableIds);
        if (selectedTableIds.isEmpty()) {
            tableScores.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                    .limit(1)
                    .map(Map.Entry::getKey)
                    .forEach(selectedTableIds::add);
            tableScoreFromColumnRecall.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                    .limit(1)
                    .map(Map.Entry::getKey)
                    .forEach(selectedTableIds::add);
        }
        return new ThresholdSelection(
                threshold,
                new ArrayList<>(selectedTableIds),
                highSimilarityColumnIdsByTable,
                highSimilarityTableIds.size(),
                highSimilarityColumnTableIds.size(),
                tableScoreFromColumnRecall.keySet().size()
        );
    }

    private DbTable selectColumns(
            DbTable table,
            Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable,
            Map<UUID, Set<UUID>> foreignKeyColumnIdsByTable
    ) {
        Set<UUID> selectedColumnIds = new LinkedHashSet<>();
        selectedColumnIds.addAll(highSimilarityColumnIdsByTable.getOrDefault(table.getId(), Set.of()));
        selectedColumnIds.addAll(foreignKeyColumnIdsByTable.getOrDefault(table.getId(), Set.of()));
        List<DbColumn> columns = table.getColumns() == null ? List.of() : table.getColumns();
        List<DbColumn> selectedColumns = selectedColumnIds.isEmpty()
                ? columns
                : columns.stream().filter(column -> selectedColumnIds.contains(column.getId())).toList();
        return new DbTable(
                table.getId(),
                table.getName(),
                table.getDescription(),
                table.getDatabaseId(),
                selectedColumns
        );
    }

    private static UUID tableIdOf(DbColumn column) {
        if (column == null) {
            return null;
        }
        if (column.getDbTable() != null) {
            return column.getDbTable().getId();
        }
        return column.getTableId();
    }

    private static String tableNameOf(DbColumn column) {
        return column == null || column.getDbTable() == null ? null : column.getDbTable().getName();
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
