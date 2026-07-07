package io.github.qifan777.server.shared.mybatis;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UuidTypeHandlerTest {

    private final UuidTypeHandler typeHandler = new UuidTypeHandler();

    @Test
    void setsUuidAsJdbcObject() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        UUID uuid = UUID.randomUUID();

        typeHandler.setNonNullParameter(statement, 1, uuid, JdbcType.OTHER);

        verify(statement).setObject(1, uuid);
    }

    @Test
    void readsUuidObject() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID uuid = UUID.randomUUID();
        when(resultSet.getObject("id")).thenReturn(uuid);

        assertThat(typeHandler.getNullableResult(resultSet, "id")).isEqualTo(uuid);
    }

    @Test
    void readsUuidString() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID uuid = UUID.randomUUID();
        when(resultSet.getObject("id")).thenReturn(uuid.toString());

        assertThat(typeHandler.getNullableResult(resultSet, "id")).isEqualTo(uuid);
    }
}
