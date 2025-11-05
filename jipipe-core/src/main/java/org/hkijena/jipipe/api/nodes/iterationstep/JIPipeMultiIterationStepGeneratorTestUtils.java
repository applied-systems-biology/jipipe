package org.hkijena.jipipe.api.nodes.iterationstep;

import com.google.common.primitives.Ints;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class JIPipeMultiIterationStepGeneratorTestUtils {
    private JIPipeMultiIterationStepGeneratorTestUtils() {

    }

    public static Map.Entry<String, Set<Integer>> slot(String name, int... rows) {
        return new AbstractMap.SimpleEntry<>(name, new HashSet<>(Ints.asList(rows)));
    }

    public static Map<String, Set<Integer>> step(Map.Entry<String, Set<Integer>>... entries) {
        Map<String, Set<Integer>> result = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> entry : entries) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static Set<Map<String, Set<Integer>>> expectedResult(Map<String, Set<Integer>>... steps) {
        return Set.of(steps);
    }

    public static DummyColumn column(String columnName, String... values) {
        return new DummyColumn(columnName, values);
    }

    /**
     * Builds a representation of the generated iteration steps
     *
     * @param generator the generator
     * @return the steps
     */
    public static Set<Map<String, Set<Integer>>> build(JIPipeMultiIterationStepGenerator generator) {
        List<JIPipeMultiIterationStep> steps = generator.build(JIPipeProgressInfo.SILENT);
        return stepsToMap(steps);
    }

    public static @NotNull HashSet<Map<String, Set<Integer>>> stepsToMap(List<JIPipeMultiIterationStep> steps) {
        var result = new HashSet<Map<String, Set<Integer>>>();
        for (JIPipeMultiIterationStep step : steps) {
            Map<String, Set<Integer>> representation = new HashMap<>();
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : step.getInputSlotRows().entrySet()) {
                representation.put(entry.getKey().getName(), entry.getValue());
            }
            result.add(representation);
        }
        return result;
    }

    public static JIPipeInputDataSlot createDummyTable(String name, DummyColumn... columns) {
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

    public static int getNumRows(DummyColumn[] columns) {
        int count = -1;
        for (DummyColumn column : columns) {
            if (count == -1) {
                count = column.numRows();
            } else if (count != column.numRows()) {
                throw new IllegalArgumentException("Wrong number of rows for column " + column.numRows());
            }
        }
        return count;
    }

    public record DummyColumn(String columnName, String... columnValues) {
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
