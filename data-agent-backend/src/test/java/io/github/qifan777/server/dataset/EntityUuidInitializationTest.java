package io.github.qifan777.server.dataset;

import io.github.qifan777.server.dataset.knowledge.domain.GlossaryKnowledge;
import io.github.qifan777.server.dataset.knowledge.domain.QuestionKnowledge;
import io.github.qifan777.server.dataset.scheme.domain.DbColumn;
import io.github.qifan777.server.dataset.scheme.domain.DbForeignKey;
import io.github.qifan777.server.dataset.scheme.domain.DbTable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityUuidInitializationTest {

    @Test
    void noArgConstructorsInitializeIds() {
        assertThat(new DbTable().getId()).isNotNull();
        assertThat(new DbColumn().getId()).isNotNull();
        assertThat(new DbForeignKey().getId()).isNotNull();
        assertThat(new QuestionKnowledge().getId()).isNotNull();
        assertThat(new GlossaryKnowledge().getId()).isNotNull();
    }
}
