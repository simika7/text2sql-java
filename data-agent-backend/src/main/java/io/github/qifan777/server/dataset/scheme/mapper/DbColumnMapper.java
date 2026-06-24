package io.github.qifan777.server.dataset.scheme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DbColumnMapper extends BaseMapper<DbColumn> {

    @Select("""
            select
                c.id,
                c.name,
                c.type,
                c.description,
                c.is_primary_key,
                c.table_id,
                t.id as table_id_result,
                t.name as table_name,
                t.description as table_description,
                t.database_id as table_database_id
            from db_column c
            join db_table t on t.id = c.table_id
            where t.database_id = #{databaseId}
            order by t.name, c.name
            """)
    @Results(id = "DbColumnWithTableResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "name", property = "name"),
            @Result(column = "type", property = "type"),
            @Result(column = "description", property = "description"),
            @Result(column = "is_primary_key", property = "isPrimaryKey"),
            @Result(column = "table_id", property = "tableId"),
            @Result(column = "table_id", property = "dbTable", javaType = DbTable.class, one = @org.apache.ibatis.annotations.One(select = "selectTableByColumnJoin")),
    })
    List<DbColumn> selectColumnsByDatabaseId(String databaseId);

    @Select("""
            select
                t.id,
                t.name,
                t.description,
                t.database_id
            from db_table t
            where t.id = #{tableId}
            """)
    DbTable selectTableByColumnJoin(java.util.UUID tableId);
}
