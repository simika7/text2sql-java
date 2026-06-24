package io.github.qifan777.server.dataset.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("glossary_knowledge")
public class GlossaryKnowledge {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String databaseId;

    private String term;

    private String description;

    private String synonyms;

    public Document toDocument() {
        String text = String.format(
                "Business term: %s. Description: %s. Synonyms: %s",
                nullToEmpty(term),
                nullToEmpty(description),
                nullToEmpty(synonyms)
        );
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vectorType", "glossaryKnowledge");
        metadata.put("databaseId", databaseId);
        metadata.put("glossaryId", id);
        metadata.put("businessTermId", id);
        return new Document(text, metadata);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
