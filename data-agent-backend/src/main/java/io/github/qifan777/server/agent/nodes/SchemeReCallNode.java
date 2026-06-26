package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchemeReCallNode implements NodeAction {

    private final VectorStore vectorStore;

    public SchemeReCallNode(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");
        List<Document> tableDocuments = retrieveTable(rewriteQuery, databaseId);
        List<Document> columnDocuments = retrieveColumn(rewriteQuery, databaseId);
        return Map.of(
                DataAgentSpec.Graph.StateKey.Recall.TABLE_SCHEMA, tableDocuments,
                DataAgentSpec.Graph.StateKey.Recall.COLUMN_SCHEMA, columnDocuments
        );
    }

    public List<Document> retrieveTable(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var expression = builder.and(
                builder.eq(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.TABLE
                ),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(expression)
                .topK(10)
                .build();
        return vectorStore.similaritySearch(request);
    }

    public List<Document> retrieveColumn(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var expression = builder.and(
                builder.eq(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.COLUMN
                ),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(expression)
                .topK(30)
                .build();
        return vectorStore.similaritySearch(request);
    }
}
