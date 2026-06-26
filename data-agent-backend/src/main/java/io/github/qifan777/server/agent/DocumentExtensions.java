package io.github.qifan777.server.agent;

import io.github.qifan777.server.dataset.knowledge.domain.GlossaryKnowledge;
import io.github.qifan777.server.dataset.knowledge.domain.QuestionKnowledge;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import org.springframework.ai.document.Document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class DocumentExtensions {

    private DocumentExtensions() {
    }

    public static UUID uuidOrNull(Map<String, Object> metadata, String key) {
        if (metadata == null || key == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return UUID.fromString(text);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    public static Document toDocument(GlossaryKnowledge knowledge) {
        if (knowledge == null) {
            return null;
        }
        return new Document(
                "业务名词: %s, 说明: %s, 同义词: %s".formatted(
                        knowledge.getTerm(),
                        knowledge.getDescription(),
                        knowledge.getSynonyms()
                ),
                metadata(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.GLOSSARY_KNOWLEDGE,
                        DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID,
                        knowledge.getDatabaseId(),
                        DataAgentSpec.Retrieval.DocumentMetadataKey.BUSINESS_TERM_ID,
                        knowledge.getId()
                )
        );
    }

    public static Document toDocument(QuestionKnowledge knowledge) {
        if (knowledge == null) {
            return null;
        }
        return new Document(
                knowledge.getQuestion(),
                metadata(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.QUESTION_KNOWLEDGE,
                        DataAgentSpec.Retrieval.DocumentMetadataKey.KNOWLEDGE_ID,
                        knowledge.getId(),
                        DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID,
                        knowledge.getDatabaseId()
                )
        );
    }

    public static Document toDocument(DbTable table) {
        if (table == null) {
            return null;
        }
        return new Document(
                table.getDescription(),
                metadata(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.TABLE,
                        DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID,
                        table.getDatabaseId(),
                        DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID,
                        table.getId()
                )
        );
    }

    public static Document toDocument(DbColumn column) {
        if (column == null) {
            return null;
        }
        DbTable table = column.getDbTable();
        return new Document(
                column.getDescription(),
                metadata(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.COLUMN,
                        DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID,
                        table == null ? null : table.getDatabaseId(),
                        DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID,
                        table == null ? column.getTableId() : table.getId(),
                        DataAgentSpec.Retrieval.DocumentMetadataKey.COLUMN_ID,
                        column.getId()
                )
        );
    }

    private static Map<String, Object> metadata(Object... keysAndValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            metadata.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return metadata;
    }
}
