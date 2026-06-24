package io.github.qifan777.server.shared.datasource;

import io.github.qifan777.server.agent.model.SqlResultSet;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResultSetBuilder {

    private ResultSetBuilder() {
    }

    public static SqlResultSet buildFrom(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnsCount = metaData.getColumnCount();

        List<String> rowHead = new ArrayList<>();
        for (int i = 1; i <= columnsCount; i++) {
            rowHead.add(metaData.getColumnLabel(i));
        }

        List<Map<String, String>> rawData = new ArrayList<>();
        int count = 0;
        while (rs.next() && count < 1000) {
            Map<String, String> kv = new LinkedHashMap<>();
            for (String h : rowHead) {
                String value = rs.getString(h);
                kv.put(h, value == null ? "" : value);
            }
            rawData.add(kv);
            count++;
        }

        return new SqlResultSet(cleanColumnNames(rowHead), cleanResultSet(rawData), null);
    }

    private static List<String> cleanColumnNames(List<String> columnNames) {
        return columnNames.stream().map(ResultSetBuilder::cleanColumnName).toList();
    }

    private static List<Map<String, String>> cleanResultSet(List<Map<String, String>> data) {
        return data.stream().map(row -> {
            Map<String, String> cleaned = new LinkedHashMap<>();
            row.forEach((key, value) -> cleaned.put(cleanColumnName(key), value));
            return cleaned;
        }).toList();
    }

    private static String cleanColumnName(String value) {
        return value == null ? null : value.replace("`", "").replace("\"", "");
    }
}
