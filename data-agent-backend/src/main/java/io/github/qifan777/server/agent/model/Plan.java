package io.github.qifan777.server.agent.model;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.shared.json.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @JsonProperty("thought_process")
    @JsonPropertyDescription("Brief analysis process")
    private String thoughtProcess;

    @JsonProperty("execution_plan")
    @JsonPropertyDescription("Execution plan steps")
    private List<PlanStep> executionPlan;

    public static Plan getPlan(OverAllState state) {
        String planStr = state.value(DataAgentSpec.Graph.StateKey.Planning.PLAN, "");
        Plan plan = JsonUtil.fromJson(planStr, Plan.class);
        if (plan == null) {
            throw new RuntimeException("plan json deserialization failed");
        }
        return plan;
    }

    public static PlanStep getCurrentStep(OverAllState state) {
        Plan plan = getPlan(state);
        Integer step = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        return plan.getExecutionPlan().get(step - 1);
    }
}
