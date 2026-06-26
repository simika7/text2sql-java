package io.github.qifan777.server.agent.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeasibilityAssessmentEdge implements EdgeAction {

    private static final Logger log = LoggerFactory.getLogger(FeasibilityAssessmentEdge.class);

    @Override
    public String apply(OverAllState state) {
        String output = state.value(DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT, "");
        if (output.contains("【需求类型】：《数据分析》")) {
            log.info("[FeasibilityAssessmentNodeEdge] need type is data analysis, route to PlannerNode");
            return DataAgentSpec.Graph.Node.PLANNER;
        }
        log.info("[FeasibilityAssessmentNodeEdge] need type is not data analysis, route to END");
        return StateGraph.END;
    }
}
