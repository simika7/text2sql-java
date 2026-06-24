package io.github.qifan777.server.dataset.scheme.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import io.github.qifan777.server.dataset.scheme.mapper.DbColumnMapper;
import io.github.qifan777.server.dataset.scheme.mapper.DbTableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DbColumnService {

    private final DbColumnMapper dbColumnMapper;
    private final DbTableMapper dbTableMapper;

    public List<DbColumn> findByDatabaseId(String databaseId) {
        return dbColumnMapper.selectColumnsByDatabaseId(databaseId);
    }

    public List<DbColumn> findByTableIdsWithTable(List<UUID> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return List.of();
        }
        List<DbColumn> columns = dbColumnMapper.selectList(new LambdaQueryWrapper<DbColumn>()
                .in(DbColumn::getTableId, tableIds));
        return attachTables(columns);
    }

    public List<DbColumn> findByIdsWithTables(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<DbColumn> columns = dbColumnMapper.selectBatchIds(ids);
        return attachTables(columns);
    }

    private List<DbColumn> attachTables(List<DbColumn> columns) {
        if (columns == null || columns.isEmpty()) {
            return List.of();
        }
        List<UUID> tableIds = columns.stream()
                .map(DbColumn::getTableId)
                .distinct()
                .toList();
        Map<UUID, DbTable> tablesById = dbTableMapper.selectBatchIds(tableIds).stream()
                .collect(Collectors.toMap(DbTable::getId, Function.identity()));
        columns.forEach(column -> column.setDbTable(tablesById.get(column.getTableId())));
        return columns;
    }
}
