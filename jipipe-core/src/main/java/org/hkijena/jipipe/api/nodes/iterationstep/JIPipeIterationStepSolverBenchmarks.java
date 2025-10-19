package org.hkijena.jipipe.api.nodes.iterationstep;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeIterationStepTextAnnotationColumMatching;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.column;
import static org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGeneratorTestUtils.createDummyTable;

public final class JIPipeIterationStepSolverBenchmarks {
    private JIPipeIterationStepSolverBenchmarks() {

    }

    public static void main(String[] args) {
        // Synthetic: 3 slots x ~50k rows, 2 reference columns, small cardinalities
        int rows = 12;
        int slots = 3;
        int groups = 200; // cardinality for #Group
        int idsPerGroup = 10; // cardinality for #ID

        List<JIPipeInputDataSlot> input = new ArrayList<>();
        Random rnd = new Random();

        for (int s = 0; s < slots; s++) {
            String[] gcol = new String[rows];
            String[] icol = new String[rows];
            for (int r = 0; r < rows; r++) {
                String g = "G" + (rnd.nextInt(groups) + 1);
                String i = String.valueOf(rnd.nextInt(idsPerGroup) + 1);
                gcol[r] = g;
                icol[r] = i;
            }
            input.add(createDummyTable("S" + (s + 1), column("#Group", gcol), column("#ID", icol)));
        }

        var baseline = new JIPipeMultiIterationStepGenerator();
        baseline.setSlots(input);
        baseline.setReferenceColumns(JIPipeIterationStepTextAnnotationColumMatching.PrefixHashUnion, new StringQueryExpression());
        baseline.setApplyMerging(true);

        // FLOW
        baseline.setSolverPreference(JIPipeIterationStepSolverPreference.ForceFlowGraph);
        long t0 = System.currentTimeMillis();
        List<JIPipeMultiIterationStep> flow = baseline.build(JIPipeProgressInfo.SILENT);
        long t1 = System.currentTimeMillis();

        // COMPOSITE
        baseline.setSolverPreference(JIPipeIterationStepSolverPreference.Auto);
        long t2 = System.currentTimeMillis();
        List<JIPipeMultiIterationStep> comp = baseline.build(JIPipeProgressInfo.SILENT);
        long t3 = System.currentTimeMillis();

        System.out.println("FlowGraph time:    " + (t1 - t0) + " ms, steps=" + flow.size());
        System.out.println("Composite time:    " + (t3 - t2) + " ms, steps=" + comp.size());
        System.out.println("Steps equal?       " + (flow.size() == comp.size()));
    }
}
