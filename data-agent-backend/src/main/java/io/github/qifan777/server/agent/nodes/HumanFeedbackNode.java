package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HumanFeedbackNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        int count = state.value(DataAgentSpec.Graph.StateKey.Planning.REPAIR_COUNT, 1);
        if (count >= 3) {
            return Map.of(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, StateGraph.END);
        }
        boolean approved = state.value(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_APPROVED, false);
        if (approved) {
            return Map.of(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, DataAgentSpec.Graph.Node.PLAN_EXECUTION);
        }
        String feedbackContent = state.value(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK, "");
        return Map.of(
                DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, DataAgentSpec.Graph.Node.PLANNER,
                DataAgentSpec.Graph.StateKey.Planning.REPAIR_COUNT, count + 1,
                DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK,
                feedbackContent.isEmpty() ? "Plan rejected by user" : feedbackContent
        );
    }
}
