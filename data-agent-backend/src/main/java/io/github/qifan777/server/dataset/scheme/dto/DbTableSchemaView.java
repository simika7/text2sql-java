package io.github.qifan777.server.dataset.scheme.dto;

import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbTableSchemaView {

    private String name;

    private List<DbColumnSchemaView> columns;

    public static DbTableSchemaView from(DbTable table) {
        if (table == null) {
            return null;
        }
        List<DbColumnSchemaView> columns = table.getColumns() == null
                ? List.of()
                : table.getColumns().stream().map(DbColumnSchemaView::from).toList();
        return new DbTableSchemaView(table.getName(), columns);
    }
}
