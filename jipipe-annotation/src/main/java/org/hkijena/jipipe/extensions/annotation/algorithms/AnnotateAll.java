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

package org.hkijena.jipipe.extensions.annotation.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPair;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that annotates all data with the same annotation
 */
@JIPipeDocumentation(name = "Set annotations", description = "Sets the specified annotations to the specified values")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Annotation, menuPath = "Modify")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class AnnotateAll extends JIPipeSimpleIteratingAlgorithm {

    private StringAndStringPair.List annotations = new StringAndStringPair.List();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    /**
     * @param info the info
     */
    public AnnotateAll(JIPipeNodeInfo info) {
        super(info);
        annotations.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public AnnotateAll(AnnotateAll other) {
        super(other);
        this.annotations = new StringAndStringPair.List(other.annotations);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (int i = 0; i < annotations.size(); i++) {
            report.forCategory("Annotations").forCategory("Item #" + (i + 1)).forCategory("Name").checkNonEmpty(annotations.get(i).getKey(), this);
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        for (StringAndStringPair annotation : annotations) {
            dataBatch.addGlobalAnnotation(new JIPipeAnnotation(annotation.getKey(), annotation.getValue()), annotationMergeStrategy);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class));
    }

    @JIPipeDocumentation(name = "Annotations", description = "Allows you to set the annotation to add/modify")
    @JIPipeParameter("generated-annotation")
    @PairParameterSettings(keyLabel = "Name", valueLabel = "Value")
    @StringParameterSettings(monospace = true)
    public StringAndStringPair.List getAnnotations() {
        return annotations;
    }

    @JIPipeParameter("generated-annotation")
    public void setAnnotations(StringAndStringPair.List annotations) {
        this.annotations = annotations;

    }

    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
