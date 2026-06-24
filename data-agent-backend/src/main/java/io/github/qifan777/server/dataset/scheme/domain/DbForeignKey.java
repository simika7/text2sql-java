package io.github.qifan777.server.dataset.scheme.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_foreign_key")
public class DbForeignKey {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private UUID sourceColumnId;

    private UUID targetColumnId;

    @TableField(exist = false)
    private DbColumn sourceColumn;

    @TableField(exist = false)
    private DbColumn targetColumn;
}
