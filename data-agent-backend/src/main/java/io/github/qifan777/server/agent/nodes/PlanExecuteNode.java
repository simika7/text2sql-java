package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.model.Plan;
import io.github.qifan777.server.agent.model.PlanStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PlanExecuteNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(PlanExecuteNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Plan plan = Plan.getPlan(state);
        int currentStep = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        List<PlanStep> steps = plan.getExecutionPlan();
        if (currentStep > steps.size()) {
            log.info("plan complete, current step is {}, total step is {}", currentStep, steps.size());
            return Map.of(
                    DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1,
                    DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, StateGraph.END
            );
        }
        return Map.of(DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, steps.get(currentStep - 1).getToolToUse());
    }
}
