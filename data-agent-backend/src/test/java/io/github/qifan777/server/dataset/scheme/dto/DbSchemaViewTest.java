package io.github.qifan777.server.dataset.scheme.dto;

import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DbSchemaViewTest {

    @Test
    void createsTableSchemaViewFromDomainTable() {
        DbColumn orderId = new DbColumn();
        orderId.setName("id");
        orderId.setType("uuid");
        orderId.setDescription("Order identifier");
        orderId.setIsPrimaryKey(true);

        DbTable orders = new DbTable();
        orders.setName("orders");
        orders.setColumns(List.of(orderId));

        DbTableSchemaView view = DbTableSchemaView.from(orders);

        assertThat(view.getName()).isEqualTo("orders");
        assertThat(view.getColumns()).singleElement().satisfies(column -> {
            assertThat(column.getName()).isEqualTo("id");
            assertThat(column.getType()).isEqualTo("uuid");
            assertThat(column.getDescription()).isEqualTo("Order identifier");
            assertThat(column.getIsPrimaryKey()).isTrue();
        });
    }

    @Test
    void rendersForeignKeyExpressionFromNestedColumns() {
        DbTable orders = table("orders");
        DbTable customers = table("customers");

        DbForeignKey foreignKey = new DbForeignKey();
        foreignKey.setSourceColumn(column("customer_id", orders));
        foreignKey.setTargetColumn(column("id", customers));

        DbForeignKeySchemaView view = DbForeignKeySchemaView.from(foreignKey);

        assertThat(view.getSourceColumn().getName()).isEqualTo("customer_id");
        assertThat(view.getSourceColumn().getDbTable().getName()).isEqualTo("orders");
        assertThat(view.getTargetColumn().getName()).isEqualTo("id");
        assertThat(view.getTargetColumn().getDbTable().getName()).isEqualTo("customers");
        assertThat(view.toExpression()).isEqualTo("orders.customer_id = customers.id");
    }

    private static DbTable table(String name) {
        DbTable table = new DbTable();
        table.setName(name);
        return table;
    }

    private static DbColumn column(String name, DbTable table) {
        DbColumn column = new DbColumn();
        column.setName(name);
        column.setDbTable(table);
        return column;
    }
}
