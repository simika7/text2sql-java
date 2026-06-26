package io.github.qifan777.server.agent.config;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.qifan777.server.agent.DataAgentSpec;
import io.github.qifan777.server.agent.edges.FeasibilityAssessmentEdge;
import io.github.qifan777.server.agent.edges.HumanFeedbackEdge;
import io.github.qifan777.server.agent.edges.PlanExecutorEdge;
import io.github.qifan777.server.agent.nodes.EvidenceRecallNode;
import io.github.qifan777.server.agent.nodes.FeasibilityAssessmentNode;
import io.github.qifan777.server.agent.nodes.HumanFeedbackNode;
import io.github.qifan777.server.agent.nodes.PlanExecuteNode;
import io.github.qifan777.server.agent.nodes.PlannerNode;
import io.github.qifan777.server.agent.nodes.PythonAnalyzeNode;
import io.github.qifan777.server.agent.nodes.PythonExecuteNode;
import io.github.qifan777.server.agent.nodes.PythonGeneratorNode;
import io.github.qifan777.server.agent.nodes.ReportGeneratorNode;
import io.github.qifan777.server.agent.nodes.SchemeReCallNode;
import io.github.qifan777.server.agent.nodes.SqlExecuteNode;
import io.github.qifan777.server.agent.nodes.SqlGeneratorNode;
import io.github.qifan777.server.agent.nodes.TableRelationNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class GraphConfiguration {

    @Bean
    public SpringAIJacksonStateSerializer serializer() {
        SpringAIJacksonStateSerializer serializer = new SpringAIJacksonStateSerializer(OverAllState::new);
        serializer.objectMapper().registerModule(new JavaTimeModule());
        return serializer;
    }

    @Bean
    public StateGraph graph(
            EvidenceRecallNode evidenceRecallNode,
            SchemeReCallNode schemeReCallNode,
            TableRelationNode tableRelationNode,
            FeasibilityAssessmentNode feasibilityAssessmentNode,
            PlannerNode plannerNode,
            StateSerializer serializer,
            HumanFeedbackNode humanFeedbackNode,
            PlanExecuteNode planExecuteNode,
            SqlGeneratorNode sqlGeneratorNode,
            SqlExecuteNode sqlExecuteNode,
            PythonGeneratorNode pythonGeneratorNode,
            PythonExecuteNode pythonExecuteNode,
            PythonAnalyzeNode pythonAnalyzeNode,
            ReportGeneratorNode reportGeneratorNode
    ) throws Exception {
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> map = new LinkedHashMap<>();
            map.put(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT, new ReplaceStrategy());

            map.put(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Recall.TABLE_SCHEMA, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Recall.COLUMN_SCHEMA, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, new ReplaceStrategy());

            map.put(DataAgentSpec.Graph.StateKey.Planning.PLAN, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Planning.REPAIR_COUNT, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, new ReplaceStrategy());

            map.put(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_APPROVED, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, new ReplaceStrategy());

            map.put(DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT, new ReplaceStrategy());
            map.put(DataAgentSpec.Graph.StateKey.Execution.REPORT_RESULT, new ReplaceStrategy());
            return map;
        };

        return new StateGraph(DataAgentSpec.GRAPH_NAME, keyStrategyFactory, serializer)
                .addNode(DataAgentSpec.Graph.Node.EVIDENCE_RECALL, node_async(evidenceRecallNode))
                .addNode(DataAgentSpec.Graph.Node.SCHEMA_RECALL, node_async(schemeReCallNode))
                .addNode(DataAgentSpec.Graph.Node.TABLE_RELATION, node_async(tableRelationNode))
                .addNode(DataAgentSpec.Graph.Node.FEASIBILITY_ASSESSMENT, node_async(feasibilityAssessmentNode))
                .addNode(DataAgentSpec.Graph.Node.PLANNER, node_async(plannerNode))
                .addNode(DataAgentSpec.Graph.Node.HUMAN_FEEDBACK, node_async(humanFeedbackNode))
                .addNode(DataAgentSpec.Graph.Node.PLAN_EXECUTION, node_async(planExecuteNode))
                .addNode(DataAgentSpec.Graph.Node.SQL_GENERATION, node_async(sqlGeneratorNode))
                .addNode(DataAgentSpec.Graph.Node.SQL_EXECUTION, node_async(sqlExecuteNode))
                .addNode(DataAgentSpec.Graph.Node.PYTHON_GENERATION, node_async(pythonGeneratorNode))
                .addNode(DataAgentSpec.Graph.Node.PYTHON_EXECUTION, node_async(pythonExecuteNode))
                .addNode(DataAgentSpec.Graph.Node.PYTHON_ANALYSIS, node_async(pythonAnalyzeNode))
                .addNode(DataAgentSpec.Graph.Node.REPORT_GENERATION, node_async(reportGeneratorNode))
                .addEdge(START, DataAgentSpec.Graph.Node.EVIDENCE_RECALL)
                .addEdge(DataAgentSpec.Graph.Node.EVIDENCE_RECALL, DataAgentSpec.Graph.Node.SCHEMA_RECALL)
                .addEdge(DataAgentSpec.Graph.Node.SCHEMA_RECALL, DataAgentSpec.Graph.Node.TABLE_RELATION)
                .addEdge(DataAgentSpec.Graph.Node.TABLE_RELATION, DataAgentSpec.Graph.Node.FEASIBILITY_ASSESSMENT)
                .addConditionalEdges(
                        DataAgentSpec.Graph.Node.FEASIBILITY_ASSESSMENT,
                        edge_async(new FeasibilityAssessmentEdge()),
                        Map.of(DataAgentSpec.Graph.Node.PLANNER, DataAgentSpec.Graph.Node.PLANNER, END, END)
                )
                .addEdge(DataAgentSpec.Graph.Node.PLANNER, DataAgentSpec.Graph.Node.HUMAN_FEEDBACK)
                .addConditionalEdges(
                        DataAgentSpec.Graph.Node.HUMAN_FEEDBACK,
                        edge_async(new HumanFeedbackEdge()),
                        Map.of(
                                END, END,
                                DataAgentSpec.Graph.Node.PLAN_EXECUTION, DataAgentSpec.Graph.Node.PLAN_EXECUTION,
                                DataAgentSpec.Graph.Node.PLANNER, DataAgentSpec.Graph.Node.PLANNER
                        )
                )
                .addConditionalEdges(
                        DataAgentSpec.Graph.Node.PLAN_EXECUTION,
                        edge_async(new PlanExecutorEdge()),
                        Map.of(
                                DataAgentSpec.Graph.Node.SQL_GENERATION, DataAgentSpec.Graph.Node.SQL_GENERATION,
                                DataAgentSpec.Graph.Node.PYTHON_GENERATION, DataAgentSpec.Graph.Node.PYTHON_GENERATION,
                                DataAgentSpec.Graph.Node.REPORT_GENERATION, DataAgentSpec.Graph.Node.REPORT_GENERATION,
                                END, END
                        )
                )
                .addEdge(DataAgentSpec.Graph.Node.SQL_GENERATION, DataAgentSpec.Graph.Node.SQL_EXECUTION)
                .addEdge(DataAgentSpec.Graph.Node.SQL_EXECUTION, DataAgentSpec.Graph.Node.PLAN_EXECUTION)
                .addEdge(DataAgentSpec.Graph.Node.PYTHON_GENERATION, DataAgentSpec.Graph.Node.PYTHON_EXECUTION)
                .addEdge(DataAgentSpec.Graph.Node.PYTHON_EXECUTION, DataAgentSpec.Graph.Node.PYTHON_ANALYSIS)
                .addEdge(DataAgentSpec.Graph.Node.PYTHON_ANALYSIS, DataAgentSpec.Graph.Node.PLAN_EXECUTION)
                .addEdge(DataAgentSpec.Graph.Node.REPORT_GENERATION, END);
    }
}
