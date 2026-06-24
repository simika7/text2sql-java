package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.model.Plan;
import io.github.qifan777.server.agent.model.PlanStep;
import io.github.qifan777.server.agent.model.Schema;
import io.github.qifan777.server.agent.prompt.PromptManager;
import io.github.qifan777.server.shared.markdown.MarkdownParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SqlGeneratorNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(SqlGeneratorNode.class);

    private final ChatModel chatModel;
    private final PromptManager promptManager;

    public SqlGeneratorNode(ChatModel chatModel, PromptManager promptManager) {
        this.chatModel = chatModel;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        PlanStep step = Plan.getCurrentStep(state);
        String instruction = step.getToolParameters().getInstruction();
        if (instruction == null) {
            throw new RuntimeException("sql generation step instruction is empty");
        }
        Schema schema = Schema.fromState(state);
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        String sqlPrompt = promptManager.newSqlGeneratorPromptTemplate.render(Map.of(
                "dialect", "mysql",
                "question", rewriteQuery,
                "schema_info", schema.buildSchemePrompt(),
                "evidence", evidence,
                "execution_description", instruction
        ));
        String sql = ChatClient.create(chatModel)
                .prompt()
                .system(sqlPrompt)
                .options(noThinkingOptions())
                .call()
                .content();
        if (sql == null) {
            throw new RuntimeException("sql generation fail");
        }
        log.info("sql {}", sql);
        return Map.of(
                DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT,
                MarkdownParserUtil.extractRawText(sql)
        );
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
