package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.DocumentExtensions;
import io.github.qifan777.server.agent.model.EvidenceQueryRewriteResult;
import io.github.qifan777.server.agent.prompt.PromptManager;
import io.github.qifan777.server.dataset.knowledge.service.QuestionKnowledgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EvidenceRecallNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(EvidenceRecallNode.class);

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final QuestionKnowledgeService questionKnowledgeService;
    private final PromptManager promptManager;

    public EvidenceRecallNode(
            ChatModel chatModel,
            VectorStore vectorStore,
            QuestionKnowledgeService questionKnowledgeService,
            PromptManager promptManager
    ) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.questionKnowledgeService = questionKnowledgeService;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String userInput = state.value(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, "");
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");
        String multiTurn = state.value(DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT, "(无)");
        BeanOutputConverter<EvidenceQueryRewriteResult> outputConverter =
                new BeanOutputConverter<>(EvidenceQueryRewriteResult.class);
        String rewritePrompt = promptManager.evidenceQueryRewritePromptTemplate.render(Map.of(
                "latest_query", userInput,
                "format", outputConverter.getFormat(),
                "multi_turn", multiTurn
        ));
        log.info("Rewrite prompt: {}", rewritePrompt);
        String rewriteResponse = ChatClient.create(chatModel)
                .prompt()
                .options(noThinkingOptions())
                .user(rewritePrompt)
                .call()
                .content();
        if (rewriteResponse == null) {
            throw new IllegalArgumentException("Invalid rewrite response");
        }
        log.info("Rewrite response: {}", rewriteResponse);
        EvidenceQueryRewriteResult converted = outputConverter.convert(rewriteResponse);
        if (converted == null) {
            throw new IllegalArgumentException("Invalid rewrite response");
        }
        String rewriteQuery = converted.getStandaloneQuery();

        List<Document> terms = retrieveGlossaryKnowledge(rewriteQuery, databaseId);
        List<Document> knowledgeDocs = retrieveKnowledge(rewriteQuery, databaseId);
        String glossaryKnowledgeText = String.join("\n", terms.stream().map(Document::getText).toList());
        List<UUID> ids = knowledgeDocs.stream()
                .map(document -> DocumentExtensions.uuidOrNull(
                        document.getMetadata(),
                        DataAgentSpec.Retrieval.DocumentMetadataKey.KNOWLEDGE_ID
                ))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        int invalidKnowledgeIdCount = knowledgeDocs.size() - ids.size();
        if (invalidKnowledgeIdCount > 0) {
            log.warn("Skipped {} recalled knowledge docs due to invalid knowledgeId metadata", invalidKnowledgeIdCount);
        }
        String questionKnowledgeText = String.join("\n", questionKnowledgeService.findByIds(ids).stream()
                .map(knowledge -> "来源：" + knowledge.getDatabaseId()
                        + " Q: " + knowledge.getQuestion()
                        + " A: " + knowledge.getAnswer())
                .toList());
        String glossaryPrompt = promptManager.businessKnowledgePromptTemplate.render(
                Map.of("businessKnowledge", glossaryKnowledgeText.isEmpty() ? "无" : glossaryKnowledgeText)
        );
        String knowledgePrompt = promptManager.agentKnowledgePromptTemplate.render(
                Map.of("agentKnowledge", questionKnowledgeText.isEmpty() ? "无" : questionKnowledgeText)
        );
        String evidence = questionKnowledgeText.isEmpty() && glossaryKnowledgeText.isEmpty()
                ? "无"
                : glossaryPrompt + "\n" + knowledgePrompt;
        return Map.of(
                DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, evidence,
                DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, rewriteQuery
        );
    }

    public List<Document> retrieveGlossaryKnowledge(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var filterExpression = builder.and(
                builder.eq(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.GLOSSARY_KNOWLEDGE
                ),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(filterExpression)
                .topK(4)
                .build();
        return vectorStore.similaritySearch(request);
    }

    public List<Document> retrieveKnowledge(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var expression = builder.and(
                builder.eq(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.QUESTION_KNOWLEDGE
                ),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(expression)
                .topK(4)
                .build();
        return vectorStore.similaritySearch(request);
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
