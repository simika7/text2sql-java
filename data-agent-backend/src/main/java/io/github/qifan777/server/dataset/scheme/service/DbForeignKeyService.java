package io.github.qifan777.server.dataset.scheme.service;

import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import io.github.qifan777.server.dataset.scheme.mapper.DbForeignKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DbForeignKeyService {

    private final DbForeignKeyMapper dbForeignKeyMapper;

    public List<DbForeignKey> findByDatabaseId(String databaseId) {
        return dbForeignKeyMapper.selectByDatabaseId(databaseId);
    }
}
