package io.github.qifan777.server.shared.datasource;

import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SqliteSchemaDataSourceProvider implements SchemaDataSourceProvider {

    public static final SqliteSchemaDataSourceProvider INSTANCE = new SqliteSchemaDataSourceProvider();

    private final ConcurrentMap<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    private SqliteSchemaDataSourceProvider() {
    }

    @Override
    public DataSource get(String databaseId) {
        return dataSourceCache.computeIfAbsent(databaseId, id -> {
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite::resource:dev_20240627/dev_databases/" + id + "/" + id + ".sqlite");
            return dataSource;
        });
    }
}
