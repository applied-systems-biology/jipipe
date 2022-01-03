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
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.functions.StringPatternExtractionFunction;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;

/**
 * Generates annotations from filenames
 */
@JIPipeDocumentation(name = "Extract & replace annotations", description = "Algorithm that allows you to extract parts of an annotation and either " +
        "replace the existing annotation or put the results into a new one.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
public class ExtractAndReplaceAnnotation extends JIPipeSimpleIteratingAlgorithm {

    private StringPatternExtractionFunction.List functions = new StringPatternExtractionFunction.List();
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public ExtractAndReplaceAnnotation(JIPipeNodeInfo info) {
        super(info);
        functions.addNewInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public ExtractAndReplaceAnnotation(ExtractAndReplaceAnnotation other) {
        super(other);
        this.functions = new StringPatternExtractionFunction.List(other.functions);
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        for (StringPatternExtractionFunction function : functions) {
            JIPipeAnnotation inputAnnotation = dataBatch.getMergedAnnotation(function.getInput());
            if (inputAnnotation == null)
                continue;
            String newValue = function.getParameter().apply(inputAnnotation.getValue());
            if (newValue == null)
                continue;
            dataBatch.addMergedAnnotation(new JIPipeAnnotation(function.getOutput(), newValue), annotationMergeStrategy);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        report.resolve("Functions").report(functions);
        for (int i = 0; i < functions.size(); i++) {
            JIPipeIssueReport subReport = report.resolve("Functions").resolve("Item #" + (i + 1));
            subReport.resolve("Input").checkNonEmpty(functions.get(i).getInput(), this);
            subReport.resolve("Output").checkNonEmpty(functions.get(i).getOutput(), this);
        }
    }

    @JIPipeDocumentation(name = "Functions", description = "The functions that allow you to extract and replace annotation values. " +
            "To extract values, you can split the incoming string into multiple components and then select the n-th component " +
            "or select the one that matches RegEx. Alternatively you can define a RegEx string that contains a matching group (brackets). " +
            "This matching group will then be picked.")
    @JIPipeParameter("functions")
    @StringParameterSettings(monospace = true)
    public StringPatternExtractionFunction.List getFunctions() {
        return functions;
    }

    @JIPipeParameter("functions")
    public void setFunctions(StringPatternExtractionFunction.List functions) {
        this.functions = functions;
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
