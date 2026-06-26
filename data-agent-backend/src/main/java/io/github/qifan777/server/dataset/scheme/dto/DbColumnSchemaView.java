package io.github.qifan777.server.dataset.scheme.dto;

import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbColumnSchemaView {

    private String name;

    private String type;

    private String description;

    private Boolean isPrimaryKey;

    public static DbColumnSchemaView from(DbColumn column) {
        if (column == null) {
            return null;
        }
        return new DbColumnSchemaView(
                column.getName(),
                column.getType(),
                column.getDescription(),
                column.getIsPrimaryKey()
        );
    }
}
