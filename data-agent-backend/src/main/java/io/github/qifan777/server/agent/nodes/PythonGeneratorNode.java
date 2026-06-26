package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.model.Plan;
import io.github.qifan777.server.agent.model.PlanStep;
import io.github.qifan777.server.agent.model.Schema;
import io.github.qifan777.server.agent.prompt.PromptManager;
import io.github.qifan777.server.shared.json.JsonUtil;
import io.github.qifan777.server.shared.markdown.MarkdownParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PythonGeneratorNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(PythonGeneratorNode.class);

    private final ChatModel chatModel;
    private final PromptManager promptManager;

    public PythonGeneratorNode(ChatModel chatModel, PromptManager promptManager) {
        this.chatModel = chatModel;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("apply PythonGeneratorNode");
        Schema schema = Schema.fromState(state);
        SqlExecuteNode.Result result = state.value(
                DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT,
                SqlExecuteNode.Result.class
        ).orElseThrow();
        PlanStep executionStep = Plan.getCurrentStep(state);
        String prompt = promptManager.pythonGeneratorPromptTemplate.render(Map.of(
                "python_memory", "500",
                "python_timeout", "500",
                "database_schema", schema.buildSchemePrompt(),
                "sample_input", JsonUtil.toJson(result.getResultSet().getData()),
                "plan_description", JsonUtil.toJson(executionStep.getToolParameters())
        ));
        String pythonCode = ChatClient.create(chatModel)
                .prompt()
                .system(prompt)
                .options(noThinkingOptions())
                .call()
                .content();
        return Map.of(
                DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT,
                MarkdownParserUtil.extractRawText(pythonCode == null ? "" : pythonCode)
        );
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
