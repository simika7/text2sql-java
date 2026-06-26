package io.github.qifan777.server.dataset.scheme.dto;

import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbForeignKeySchemaView {

    private ColumnRef sourceColumn;

    private ColumnRef targetColumn;

    public static DbForeignKeySchemaView from(DbForeignKey foreignKey) {
        if (foreignKey == null) {
            return null;
        }
        return new DbForeignKeySchemaView(
                ColumnRef.from(foreignKey.getSourceColumn()),
                ColumnRef.from(foreignKey.getTargetColumn())
        );
    }

    public String toExpression() {
        return "%s.%s = %s.%s".formatted(
                sourceColumn.getDbTable().getName(),
                sourceColumn.getName(),
                targetColumn.getDbTable().getName(),
                targetColumn.getName()
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnRef {

        private String name;

        private TableRef dbTable;

        public static ColumnRef from(DbColumn column) {
            if (column == null) {
                return null;
            }
            return new ColumnRef(column.getName(), TableRef.from(column.getDbTable()));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableRef {

        private String name;

        public static TableRef from(DbTable table) {
            if (table == null) {
                return null;
            }
            return new TableRef(table.getName());
        }
    }
}
