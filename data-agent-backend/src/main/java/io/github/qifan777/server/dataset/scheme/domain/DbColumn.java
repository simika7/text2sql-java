package io.github.qifan777.server.dataset.scheme.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.document.Document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_column")
public class DbColumn {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String name;

    private String type;

    private String description;

    private Boolean isPrimaryKey;

    private UUID tableId;

    @TableField(exist = false)
    private DbTable dbTable;

    public Document toDocument() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vectorType", "column");
        metadata.put("databaseId", dbTable == null ? null : dbTable.getDatabaseId());
        metadata.put("tableId", tableId);
        metadata.put("columnId", id);

        String tableName = dbTable == null ? "" : nullToEmpty(dbTable.getName());
        String text = String.format(
                "Column: %s.%s. Type: %s. Primary key: %s. Description: %s",
                tableName,
                nullToEmpty(name),
                nullToEmpty(type),
                Boolean.TRUE.equals(isPrimaryKey),
                nullToEmpty(description)
        );
        return new Document(text, metadata);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
