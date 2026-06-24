package io.github.qifan777.server.shared.datasource;

import javax.sql.DataSource;

@FunctionalInterface
public interface SchemaDataSourceProvider {

    DataSource get(String databaseId);
}
