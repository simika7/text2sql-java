package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.prompt.PromptManager;
import io.github.qifan777.server.shared.python.PythonExecutionResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PythonAnalyzeNode implements NodeAction {

    private final ChatModel chatModel;
    private final PromptManager promptManager;

    public PythonAnalyzeNode(ChatModel chatModel, PromptManager promptManager) {
        this.chatModel = chatModel;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        PythonExecutionResult pythonOutput = state.value(
                DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT,
                PythonExecutionResult.class
        ).orElseThrow();
        int step = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        Map<String, String> executionOutput = new LinkedHashMap<>(
                state.<Map<String, String>>value(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT)
                        .orElseThrow()
        );
        String prompt = promptManager.pythonAnalyzePromptTemplate.render(Map.of(
                "python_output", pythonOutput,
                "user_query", rewriteQuery
        ));
        String analyze = ChatClient.create(chatModel)
                .prompt()
                .options(noThinkingOptions())
                .user(prompt)
                .call()
                .content();
        executionOutput.put("step_" + step + "_analysis", analyze == null ? "" : analyze);
        return Map.of(
                DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, executionOutput,
                DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, step + 1
        );
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
