package io.github.qifan777.server.shared.python;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonExecutionResult {

    private boolean success;

    private String output;

    private String error;
}
