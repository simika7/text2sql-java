package io.github.qifan777.server.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.model.DisplaySpec;
import io.github.qifan777.server.agent.model.Plan;
import io.github.qifan777.server.agent.model.SqlResultSet;
import io.github.qifan777.server.shared.datasource.ResultSetBuilder;
import io.github.qifan777.server.shared.datasource.SqliteSchemaDataSourceProvider;
import io.github.qifan777.server.shared.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SqlExecuteNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(SqlExecuteNode.class);

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        int currentStep = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        String sql = state.value(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT, "");
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");
        SqlResultSet resultSetWrapper;
        try (Connection connection = SqliteSchemaDataSourceProvider.INSTANCE.get(databaseId).getConnection()) {
            resultSetWrapper = ResultSetBuilder.buildFrom(connection.createStatement().executeQuery(sql));
        }
        log.info("sql execute {}", resultSetWrapper);
        DisplaySpec displaySpec = buildDisplaySpec(resultSetWrapper);
        log.info("display spec: {}", displaySpec);
        Plan.getCurrentStep(state).getToolParameters().setSqlQuery(sql);
        Map<String, String> executionOutput = new LinkedHashMap<>();
        executionOutput.put("step_" + currentStep, JsonUtil.toJson(resultSetWrapper));
        return Map.of(
                DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, currentStep + 1,
                DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT, new Result(resultSetWrapper, displaySpec),
                DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, executionOutput
        );
    }

    private DisplaySpec buildDisplaySpec(SqlResultSet resultSetWrapper) {
        List<String> columns = resultSetWrapper.getColumn() == null ? List.of() : resultSetWrapper.getColumn();
        if (columns.isEmpty()) {
            return new DisplaySpec("table", "SQL已生成，等待外部执行", null, List.of());
        }
        return new DisplaySpec("table", "SQL执行结果", columns.getFirst(), columns.stream().skip(1).toList());
    }

    public static class Result {
        private SqlResultSet resultSet;
        private DisplaySpec display;

        public Result() {
        }

        public Result(SqlResultSet resultSet, DisplaySpec display) {
            this.resultSet = resultSet;
            this.display = display;
        }

        public SqlResultSet getResultSet() {
            return resultSet;
        }

        public void setResultSet(SqlResultSet resultSet) {
            this.resultSet = resultSet;
        }

        public DisplaySpec getDisplay() {
            return display;
        }

        public void setDisplay(DisplaySpec display) {
            this.display = display;
        }
    }
}
