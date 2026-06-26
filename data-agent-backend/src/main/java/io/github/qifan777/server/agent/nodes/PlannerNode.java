package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.model.Plan;
import io.github.qifan777.server.agent.model.Schema;
import io.github.qifan777.server.agent.prompt.PromptManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlannerNode implements NodeAction {

    private final ChatModel chatModel;
    private final PromptManager promptManager;

    public PlannerNode(ChatModel chatModel, PromptManager promptManager) {
        this.chatModel = chatModel;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        Schema schema = Schema.fromState(state);
        String feedbackContent = state.value(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK, "");
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        BeanOutputConverter<Plan> beanOutputConverter = new BeanOutputConverter<>(Plan.class);
        String prompt = promptManager.plannerPromptTemplate.render(Map.of(
                "user_question", rewriteQuery,
                "schema", schema.buildSchemePrompt(),
                "evidence", evidence,
                "semantic_model", "",
                "plan_validation_error", feedbackContent,
                "format", beanOutputConverter.getFormat()
        ));
        String plan = ChatClient.create(chatModel)
                .prompt()
                .options(noThinkingOptions())
                .user(prompt)
                .call()
                .content();
        if (plan == null) {
            throw new RuntimeException("planner fail");
        }
        return Map.of(DataAgentSpec.Graph.StateKey.Planning.PLAN, plan);
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
