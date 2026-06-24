package io.github.qifan777.server.dataset.scheme.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import io.github.qifan777.server.dataset.scheme.mapper.DbTableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DbTableService {

    private final DbTableMapper dbTableMapper;
    private final DbColumnService dbColumnService;

    public List<DbTable> findByDatabaseId(String databaseId) {
        return dbTableMapper.selectList(new LambdaQueryWrapper<DbTable>()
                .eq(DbTable::getDatabaseId, databaseId));
    }

    public List<DbTable> findByDatabaseIdAndNames(String databaseId, List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        List<DbTable> tables = dbTableMapper.selectList(new LambdaQueryWrapper<DbTable>()
                .eq(DbTable::getDatabaseId, databaseId)
                .in(DbTable::getName, names));
        return attachColumns(tables);
    }

    public List<DbTable> findByIdsWithColumns(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<DbTable> tables = dbTableMapper.selectBatchIds(ids);
        return attachColumns(tables);
    }

    public List<DbTable> attachColumns(List<DbTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return List.of();
        }
        List<UUID> tableIds = tables.stream()
                .map(DbTable::getId)
                .toList();
        Map<UUID, List<DbColumn>> columnsByTableId = dbColumnService.findByTableIdsWithTable(tableIds).stream()
                .collect(Collectors.groupingBy(DbColumn::getTableId));
        tables.forEach(table -> table.setColumns(new ArrayList<>(
                columnsByTableId.getOrDefault(table.getId(), List.of())
        )));
        return tables;
    }
}
