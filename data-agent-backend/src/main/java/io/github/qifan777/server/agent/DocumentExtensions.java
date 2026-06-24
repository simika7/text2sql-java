package io.github.qifan777.server.agent;

import io.github.qifan777.server.dataset.knowledge.domain.GlossaryKnowledge;
import io.github.qifan777.server.dataset.knowledge.domain.QuestionKnowledge;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import org.springframework.ai.document.Document;

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
        return knowledge == null ? null : knowledge.toDocument();
    }

    public static Document toDocument(QuestionKnowledge knowledge) {
        return knowledge == null ? null : knowledge.toDocument();
    }

    public static Document toDocument(DbTable table) {
        return table == null ? null : table.toDocument();
    }

    public static Document toDocument(DbColumn column) {
        return column == null ? null : column.toDocument();
    }
}
