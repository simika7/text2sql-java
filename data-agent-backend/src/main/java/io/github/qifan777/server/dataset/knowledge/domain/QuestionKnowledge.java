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
@TableName("question_knowledge")
public class QuestionKnowledge {

    @TableId(type = IdType.INPUT)
    private UUID id = UUID.randomUUID();

    private String databaseId;

    private String question;

    private String answer;

    public Document toDocument() {
        String text = String.format("Question: %s%nAnswer: %s", nullToEmpty(question), nullToEmpty(answer));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vectorType", "questionKnowledge");
        metadata.put("databaseId", databaseId);
        metadata.put("questionId", id);
        metadata.put("knowledgeId", id);
        return new Document(text, metadata);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
