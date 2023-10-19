package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;

import java.util.*;

/**
 * Result of a data batch generation run
 */
public class JIPipeDataBatchGenerationResult {
    private List<JIPipeMultiIterationStep> iterationSteps = new ArrayList<>();
    private Set<String> referenceTextAnnotationColumns = new HashSet<>();

    public List<JIPipeMultiIterationStep> getDataBatches() {
        return iterationSteps;
    }

    public void setDataBatches(List<JIPipeMultiIterationStep> iterationSteps) {
        this.iterationSteps = iterationSteps;
    }

    public Set<String> getReferenceTextAnnotationColumns() {
        return referenceTextAnnotationColumns;
    }

    public void setReferenceTextAnnotationColumns(Set<String> referenceTextAnnotationColumns) {
        this.referenceTextAnnotationColumns = referenceTextAnnotationColumns;
    }

    public void setDataBatches(JIPipeMultiIterationStep... iterationSteps) {
        this.iterationSteps = new ArrayList<>();
        for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
            this.iterationSteps.add(iterationStep);
        }
    }
}
