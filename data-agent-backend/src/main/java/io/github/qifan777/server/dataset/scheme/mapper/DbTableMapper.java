package io.github.qifan777.server.dataset.scheme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DbTableMapper extends BaseMapper<DbTable> {
}
