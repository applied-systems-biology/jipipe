package org.hkijena.jipipe.api.nodes.iterationstep.solvers;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.IterationStepSolver;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;

import java.util.ArrayList;
import java.util.List;

public class SplitAllSolver implements IterationStepSolver {
    @Override
    public List<JIPipeMultiIterationStep> solve(JIPipeMultiIterationStepGenerator generator, JIPipeProgressInfo progressInfo) {
        List<JIPipeMultiIterationStep> split = new ArrayList<>();
        for (JIPipeDataSlot slot : generator.getSlots()) {
            for (int row = 0; row < slot.getRowCount(); row++) {
                if (row % 1000 == 0) {
                    progressInfo.resolveAndLog("Row", row, slot.getRowCount());
                    if (progressInfo.isCancelled())
                        return null;
                }
                JIPipeMultiIterationStep batch = new JIPipeMultiIterationStep(generator.getNode());
                for (JIPipeDataSlot slot2 : generator.getSlots()) {
                    batch.addEmptySlot(slot2);
                }
                batch.addInputData(slot, row);
                batch.addMergedTextAnnotations(slot.getTextAnnotations(row), generator.getAnnotationMergeStrategy());
                batch.addMergedDataAnnotations(slot.getDataAnnotations(row), generator.getDataAnnotationMergeStrategy());
                split.add(batch);
            }
        }
        return split;
    }
}
