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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_table")
public class DbTable {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private String name;

    private String description;

    private String databaseId;

    @TableField(exist = false)
    private List<DbColumn> columns;

    public Document toDocument() {
        String text = String.format("Table: %s. Description: %s", nullToEmpty(name), nullToEmpty(description));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("vectorType", "table");
        metadata.put("databaseId", databaseId);
        metadata.put("tableId", id);
        return new Document(text, metadata);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
