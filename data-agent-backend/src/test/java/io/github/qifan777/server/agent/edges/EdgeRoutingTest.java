package io.github.qifan777.server.agent.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import io.github.qifan777.server.agent.DataAgentSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeRoutingTest {

    @Test
    void feasibilityRoutesDataAnalysisToPlanner() throws Exception {
        OverAllState state = new OverAllState(Map.of(
                DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT,
                "【需求类型】：《数据分析》"
        ));

        assertThat(new FeasibilityAssessmentEdge().apply(state))
                .isEqualTo(DataAgentSpec.Graph.Node.PLANNER);
    }

    @Test
    void feasibilityRoutesOtherNeedsToEnd() throws Exception {
        OverAllState state = new OverAllState(Map.of(
                DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT,
                "【需求类型】：《闲聊》"
        ));

        assertThat(new FeasibilityAssessmentEdge().apply(state))
                .isEqualTo(StateGraph.END);
    }

    @Test
    void humanFeedbackAndPlanExecutorReturnNextNodeOrEnd() throws Exception {
        OverAllState humanState = new OverAllState(Map.of(
                DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE,
                DataAgentSpec.Graph.Node.PLAN_EXECUTION
        ));
        OverAllState planState = new OverAllState(Map.of(
                DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE,
                DataAgentSpec.Graph.Node.SQL_GENERATION
        ));

        assertThat(new HumanFeedbackEdge().apply(humanState))
                .isEqualTo(DataAgentSpec.Graph.Node.PLAN_EXECUTION);
        assertThat(new HumanFeedbackEdge().apply(new OverAllState()))
                .isEqualTo(StateGraph.END);
        assertThat(new PlanExecutorEdge().apply(planState))
                .isEqualTo(DataAgentSpec.Graph.Node.SQL_GENERATION);
        assertThat(new PlanExecutorEdge().apply(new OverAllState()))
                .isEqualTo(StateGraph.END);
    }
}
