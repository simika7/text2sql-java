package io.github.qifan777.server.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlResultSet implements Cloneable {

    private List<String> column = new ArrayList<>();

    private List<Map<String, String>> data = new ArrayList<>();

    private String errorMsg;

    @Override
    public SqlResultSet clone() {
        List<String> columnCopy = column == null ? null : new ArrayList<>(column);
        List<Map<String, String>> dataCopy = data == null
                ? null
                : data.stream().map(HashMap::new).map(row -> (Map<String, String>) row).toList();
        return new SqlResultSet(columnCopy, dataCopy, null);
    }
}
