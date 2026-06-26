package io.github.qifan777.server.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceQueryRewriteResult {

    @JsonProperty("standalone_query")
    @JsonPropertyDescription("Rewritten standalone query")
    private String standaloneQuery = "";
}
