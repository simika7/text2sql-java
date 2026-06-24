package io.github.qifan777.server.agent.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisplaySpec {

    @JsonPropertyDescription("Chart type, such as table, bar, line, or pie")
    private String type;

    @JsonPropertyDescription("Chart title")
    private String title;

    @JsonPropertyDescription("X axis field name")
    private String x;

    @JsonPropertyDescription("Y axis field names")
    private List<String> y;
}
