package io.github.qifan777.server.dataset.scheme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DbForeignKeyMapper extends BaseMapper<DbForeignKey> {

    List<DbForeignKey> selectByDatabaseId(String databaseId);
}
