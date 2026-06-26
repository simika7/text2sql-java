package io.github.qifan777.server.agent.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import io.github.qifan777.server.agent.DataAgentSpec;

public class HumanFeedbackEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        return state.value(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, StateGraph.END);
    }
}
