package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class JIPipeMultiIterationStepGeneratorTest {
    /**
     * A basic test that applies a simple iteration case (1 input of N items = N iteration steps)
     * Uses the default selection method (use # to denote reference columns)
     */
    @Test
    public void testSimpleIteration() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "B", "C"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(false);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);
        assertEquals(3, result.size());
    }

    /**
     * A basic test that applies a simple merging case, but all reference column values are different
     * Uses the default selection method (use # to denote important columns)
     */
    @Test
    public void testSimpleMergingIteration1() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "B", "C"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(true);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);
        assertEquals(3, result.size());
    }

    /**
     * A basic test that applies a simple merging case, where we should receive (A A) and (B)
     * Uses the default selection method (use # to denote important columns)
     */
    @Test
    public void testSimpleMergingIteration2() {
        JIPipeMultiIterationStepGenerator generator = new JIPipeMultiIterationStepGenerator();
        generator.setSlots(List.of(createDummyTable("Input", column("#Dataset", "A", "A", "B"))));
        generator.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        generator.setApplyMerging(true);
        List<JIPipeMultiIterationStep> result = generator.build(JIPipeProgressInfo.SILENT);
        assertEquals(2, result.size());
    }

    private static DummyColumn column(String columnName, String... values) {
        return new DummyColumn(columnName, values);
    }

    private static JIPipeInputDataSlot createDummyTable(String name, DummyColumn... columns) {
        final int numRows = getNumRows(columns);
        JIPipeInputDataSlot dummySlot = new DummySlot(new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, name, ""), null);

        for (int row = 0; row < numRows; row++) {
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            for (DummyColumn column : columns) {
               annotations.add(new JIPipeTextAnnotation(column.columnName(), column.columnValues()[row]));
            }
            dummySlot.addData(new JIPipeEmptyData(),
                    annotations,
                    JIPipeTextAnnotationMergeMode.OverwriteExisting,
                    Collections.emptyList(),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting,
                    JIPipeProgressInfo.SILENT);
        }

        return dummySlot;
    }

    private static int getNumRows(DummyColumn[] columns) {
        int count = -1;
        for (DummyColumn column : columns) {
            if(count == -1) {
                count = column.numRows();
            }
            else if(count != column.numRows()) {
                throw new IllegalArgumentException("Wrong number of rows for column " + column.numRows());
            }
        }
        return count;
    }

    private record DummyColumn(String columnName, String... columnValues) {
        int numRows() {
            return columnValues().length;
        }
    }

    /**
     * A dummy slot that does not invoke JIPipe for testing data acceptance
     */
    public static class DummySlot extends JIPipeInputDataSlot {

        public DummySlot(JIPipeDataSlotInfo info, JIPipeGraphNode node) {
            super(info, node);
        }

        @Override
        public boolean accepts(JIPipeData data) {
            return true;
        }

        @Override
        public boolean accepts(Class<? extends JIPipeData> klass) {
            return true;
        }
    }
}