package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.model.Schema;
import io.github.qifan777.server.agent.prompt.PromptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FeasibilityAssessmentNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(FeasibilityAssessmentNode.class);

    private final ChatModel chatModel;
    private final PromptManager promptManager;

    public FeasibilityAssessmentNode(ChatModel chatModel, PromptManager promptManager) {
        this.chatModel = chatModel;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        Schema schema = Schema.fromState(state);
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        String multiTurn = state.value(DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT, "(无)");
        String prompt = promptManager.feasibilityAssessmentPromptTemplate.render(Map.of(
                "recalled_schema", schema.buildSchemePrompt(),
                "evidence", evidence,
                "canonical_query", rewriteQuery,
                "multi_turn", multiTurn
        ));
        String feasibilityAssessment = ChatClient.create(chatModel)
                .prompt()
                .options(noThinkingOptions())
                .user(prompt)
                .call()
                .content();
        if (feasibilityAssessment == null) {
            throw new RuntimeException("feasible assessment fail");
        }
        log.info("feasibilityAssessment: {}", feasibilityAssessment);
        return Map.of(DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT, feasibilityAssessment);
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
