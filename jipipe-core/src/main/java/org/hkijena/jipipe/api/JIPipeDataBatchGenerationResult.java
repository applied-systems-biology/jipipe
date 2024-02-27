/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

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
