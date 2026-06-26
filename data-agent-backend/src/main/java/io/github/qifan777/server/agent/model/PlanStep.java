package io.github.qifan777.server.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanStep {

    @JsonProperty("step")
    @JsonPropertyDescription("Step order")
    private int step;

    @JsonProperty("tool_to_use")
    @JsonPropertyDescription("Tool name")
    private String toolToUse;

    @JsonProperty("tool_parameters")
    @JsonPropertyDescription("Tool parameters")
    private ToolParameters toolParameters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolParameters {

        @JsonProperty("instruction")
        @JsonPropertyDescription("Instruction for SQL or Python generation")
        private String instruction;

        @JsonProperty("summary_and_recommendations")
        @JsonPropertyDescription("Summary and recommendations for report generation")
        private String summaryAndRecommendations;

        @JsonProperty("sql_query")
        @JsonPropertyDescription("Generated SQL query")
        private String sqlQuery;
    }
}
