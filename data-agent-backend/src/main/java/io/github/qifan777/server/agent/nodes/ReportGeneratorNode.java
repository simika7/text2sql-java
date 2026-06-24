package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.model.Plan;
import io.github.qifan777.server.agent.model.PlanStep;
import io.github.qifan777.server.agent.prompt.PromptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
public class ReportGeneratorNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(ReportGeneratorNode.class);

    public static final String CLEAN_JSON_EXAMPLE = """
            {
                "title": { "text": "月度销售额" },
                "tooltip": { "trigger": "axis" },
                "xAxis": { "type": "category", "data": ["1月", "2月"] },
                "yAxis": { "type": "value" },
                "series": [
                    { "type": "bar", "data": [120, 200] }
                ]
            }
            """.stripIndent();

    private final ChatModel chatModel;
    private final PromptManager promptManager;

    public ReportGeneratorNode(ChatModel chatModel, PromptManager promptManager) {
        this.chatModel = chatModel;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("apply ReportGeneratorNode");
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        Plan plan = Plan.getPlan(state);
        Map<String, String> result = state.<Map<String, String>>value(
                DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT
        ).orElseThrow();
        String summaryAndRecommendations = Plan.getCurrentStep(state)
                .getToolParameters()
                .getSummaryAndRecommendations();
        if (summaryAndRecommendations == null) {
            throw new RuntimeException("report summary and recommendations is empty");
        }
        return Map.of(
                DataAgentSpec.Graph.StateKey.Execution.REPORT_RESULT,
                generateReport(rewriteQuery, plan, result, summaryAndRecommendations)
        );
    }

    private Flux<ChatResponse> generateReport(
            String userInput,
            Plan plan,
            Map<String, String> executionResults,
            String summaryAndRecommendations
    ) {
        String userRequirementsAndPlan = buildUserRequirementsAndPlan(userInput, plan);
        String analysisStepsAndData = buildAnalysisStepsAndData(plan, executionResults);
        String reportPrompt = promptManager.reportGeneratorPlainPromptTemplate.render(Map.of(
                "user_requirements_and_plan", userRequirementsAndPlan,
                "analysis_steps_and_data", analysisStepsAndData,
                "summary_and_recommendations", summaryAndRecommendations,
                "json_example", CLEAN_JSON_EXAMPLE,
                "optimization_section", ""
        ));
        log.info("Report Node Prompt: \n {} \n", reportPrompt);
        return ChatClient.create(chatModel)
                .prompt()
                .options(noThinkingOptions())
                .user(reportPrompt)
                .stream()
                .chatResponse();
    }

    private String buildUserRequirementsAndPlan(String userInput, Plan plan) {
        StringBuilder builder = new StringBuilder();
        builder.append("## 用户原始需求\n");
        builder.append(userInput).append("\n\n");
        builder.append("## 执行计划概述\n");
        builder.append("**思考过程**: ").append(plan.getThoughtProcess()).append("\n\n");
        builder.append("## 详细执行步骤\n");
        List<PlanStep> executionPlan = plan.getExecutionPlan() == null ? List.of() : plan.getExecutionPlan();
        for (int i = 0; i < executionPlan.size(); i++) {
            PlanStep step = executionPlan.get(i);
            builder.append("### 步骤 ").append(i + 1).append(": 步骤编号 ").append(step.getStep()).append("\n");
            builder.append("**工具**: ").append(step.getToolToUse()).append("\n");
            if (step.getToolParameters() != null) {
                builder.append("**参数描述**: ").append(step.getToolParameters().getInstruction()).append("\n");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String buildAnalysisStepsAndData(Plan plan, Map<String, String> executionResults) {
        StringBuilder builder = new StringBuilder();
        builder.append("## 数据执行结果\n");
        if (executionResults.isEmpty()) {
            builder.append("暂无执行结果数据\n");
            return builder.toString();
        }
        List<PlanStep> executionPlan = plan.getExecutionPlan() == null ? List.of() : plan.getExecutionPlan();
        executionResults.forEach((stepKey, stepResult) -> {
            if (stepKey.endsWith("_analysis")) {
                return;
            }
            builder.append("### ").append(stepKey).append("\n");
            try {
                int stepIndex = Integer.parseInt(stepKey.replace("step_", "")) - 1;
                if (stepIndex >= 0 && stepIndex < executionPlan.size()) {
                    PlanStep step = executionPlan.get(stepIndex);
                    builder.append("**步骤编号**: ").append(step.getStep()).append("\n");
                    builder.append("**使用工具**: ").append(step.getToolToUse()).append("\n");
                    if (step.getToolParameters() != null) {
                        builder.append("**参数描述**: ").append(step.getToolParameters().getInstruction()).append("\n");
                        if (step.getToolParameters().getSqlQuery() != null) {
                            builder.append("**执行SQL**: \n```sql\n")
                                    .append(step.getToolParameters().getSqlQuery())
                                    .append("\n```\n");
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed step keys from prior execution state.
            }
            builder.append("**执行结果**: \n```json\n").append(stepResult).append("\n```\n\n");
            String analysisResult = executionResults.get(stepKey + "_analysis");
            if (analysisResult != null && !analysisResult.isBlank()) {
                builder.append("**Python 分析结果**: ").append(analysisResult).append(' ');
            }
        });
        return builder.toString();
    }

    private OpenAiChatOptions noThinkingOptions() {
        return OpenAiChatOptions.builder()
                .extraBody(Map.of("enable_thinking", false))
                .build();
    }
}
