package org.hkijena.jipipe.api.nodes.iterationstep.solvers;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepSolver;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;

import java.util.ArrayList;
import java.util.List;

public class MergeAllSolver implements JIPipeIterationStepSolver {
    @Override
    public List<JIPipeMultiIterationStep> solve(JIPipeMultiIterationStepGenerator generator, JIPipeProgressInfo progressInfo) {
        JIPipeMultiIterationStep batch = new JIPipeMultiIterationStep(generator.getNode());
        List<JIPipeDataSlot> slotList = generator.getSlots();

        // Handle case where there is no data
        boolean hasData = false;
        for (JIPipeDataSlot slot : slotList) {
            if (!slot.isEmpty()) {
                hasData = true;
            }
        }
        if (!hasData) {
            progressInfo.log("Info: no data present. Nothing to do.");
            return new ArrayList<>();
        }

        for (JIPipeDataSlot slot : slotList) {
            batch.addEmptySlot(slot);
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
            for (int row = 0; row < slot.getRowCount(); row++) {
                if (row % 1000 == 0) {
                    progressInfo.resolveAndLog("Row", row, slot.getRowCount());
                    if (progressInfo.isCancelled())
                        return null;
                }
                batch.addInputData(slot, row);
                annotations.addAll(slot.getTextAnnotations(row));
                dataAnnotations.addAll(slot.getDataAnnotations(row));
            }
            progressInfo.log("Merging " + annotations.size() + " annotations");
            batch.addMergedTextAnnotations(annotations, generator.getAnnotationMergeStrategy());
            batch.addMergedDataAnnotations(dataAnnotations, generator.getDataAnnotationMergeStrategy());
        }
        return new ArrayList<>(List.of(batch));
    }
}
