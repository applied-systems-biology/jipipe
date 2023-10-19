/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes.algorithm;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class JIPipeSingleIterationAlgorithmIterationStepGenerationSettings extends AbstractJIPipeParameterCollection implements JIPipeIterationStepGenerationSettings {
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.Merge;
    private JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy = JIPipeDataAnnotationMergeMode.MergeTables;

    public JIPipeSingleIterationAlgorithmIterationStepGenerationSettings() {
    }

    public JIPipeSingleIterationAlgorithmIterationStepGenerationSettings(JIPipeSingleIterationAlgorithmIterationStepGenerationSettings other) {
        this.annotationMergeStrategy = other.annotationMergeStrategy;
        this.dataAnnotationMergeStrategy = other.dataAnnotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if data sets that " +
            "define different values for the same annotation columns are encountered.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }

    @JIPipeDocumentation(name = "Merge same data annotation values", description = "Determines which strategy is applied if different values for the same data annotation columns are encountered.")
    @JIPipeParameter("data-annotation-merge-strategy")
    public JIPipeDataAnnotationMergeMode getDataAnnotationMergeStrategy() {
        return dataAnnotationMergeStrategy;
    }

    @JIPipeParameter("data-annotation-merge-strategy")
    public void setDataAnnotationMergeStrategy(JIPipeDataAnnotationMergeMode dataAnnotationMergeStrategy) {
        this.dataAnnotationMergeStrategy = dataAnnotationMergeStrategy;
    }
}
