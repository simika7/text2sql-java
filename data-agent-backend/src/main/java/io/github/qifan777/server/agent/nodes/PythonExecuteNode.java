package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.shared.json.JsonUtil;
import io.github.qifan777.server.shared.python.SimplePythonExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PythonExecuteNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String pythonCode = state.value(DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT, "");
        SqlExecuteNode.Result result = state.value(
                DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT,
                SqlExecuteNode.Result.class
        ).orElseThrow();
        String inputDataJson = JsonUtil.toJson(result.getResultSet().getData());
        if (inputDataJson == null) {
            throw new RuntimeException("sql result is empty");
        }
        return Map.of(
                DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT,
                SimplePythonExecutor.execute(pythonCode, inputDataJson)
        );
    }
}
