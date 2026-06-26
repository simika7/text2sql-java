package io.github.qifan777.server.agent.nodes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TableRelationNodeTest {

    @Test
    void thresholdKeepsBaseWhenTableCountIsAtOrBelowTarget() {
        UUID tableA = UUID.randomUUID();
        UUID tableB = UUID.randomUUID();
        UUID columnB = UUID.randomUUID();

        TableRelationNode.ThresholdSelection selection = TableRelationNode.selectThreshold(
                Map.of(tableA, 0.41),
                List.of(new TableRelationNode.ColumnScore(tableB, columnB, 0.42))
        );

        assertThat(selection.threshold()).isEqualTo(0.4);
        assertThat(selection.tableIds()).containsExactly(tableA, tableB);
        assertThat(selection.highSimilarityColumnIdsByTable())
                .containsEntry(tableB, java.util.Set.of(columnB));
    }

    @Test
    void thresholdRaisesByOnePercentStepsTowardTargetTableCount() {
        UUID table1 = UUID.randomUUID();
        UUID table2 = UUID.randomUUID();
        UUID table3 = UUID.randomUUID();
        UUID table4 = UUID.randomUUID();
        UUID table5 = UUID.randomUUID();

        TableRelationNode.ThresholdSelection selection = TableRelationNode.selectThreshold(
                Map.of(
                        table1, 0.45,
                        table2, 0.44,
                        table3, 0.43,
                        table4, 0.42,
                        table5, 0.41
                ),
                List.of()
        );

        assertThat(selection.threshold()).isCloseTo(0.41, org.assertj.core.data.Offset.offset(0.000001));
        assertThat(selection.tableIds()).containsExactlyInAnyOrder(table1, table2, table3, table4);
    }

    @Test
    void thresholdFallsBackToTopTableAndTopColumnRecallWhenNoScorePasses() {
        UUID topTable = UUID.randomUUID();
        UUID lowerTable = UUID.randomUUID();
        UUID columnTable = UUID.randomUUID();
        UUID topColumn = UUID.randomUUID();
        UUID lowerColumn = UUID.randomUUID();

        TableRelationNode.ThresholdSelection selection = TableRelationNode.selectThreshold(
                Map.of(topTable, 0.30, lowerTable, 0.20),
                List.of(
                        new TableRelationNode.ColumnScore(columnTable, topColumn, 0.35),
                        new TableRelationNode.ColumnScore(UUID.randomUUID(), lowerColumn, 0.10)
                )
        );

        assertThat(selection.threshold()).isEqualTo(0.4);
        assertThat(selection.tableIds()).containsExactly(topTable, columnTable);
        assertThat(selection.highSimilarityColumnIdsByTable()).isEmpty();
    }
}
