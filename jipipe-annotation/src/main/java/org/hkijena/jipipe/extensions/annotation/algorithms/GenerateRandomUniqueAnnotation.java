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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates annotations from filenames
 */
@JIPipeDocumentation(name = "Random annotation", description = "Generates a unique random numeric annotation. " +
        "The value range is [0, number of rows]. This is useful for randomly distributing data.")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For all data")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Annotated data", inheritedSlot = "Data", autoCreate = true)
public class GenerateRandomUniqueAnnotation extends JIPipeParameterSlotAlgorithm {

    private String generatedAnnotation = "#Random";
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public GenerateRandomUniqueAnnotation(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public GenerateRandomUniqueAnnotation(GenerateRandomUniqueAnnotation other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        List<Integer> availableRows = new ArrayList<>();
        for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
            availableRows.add(i);
        }
        Collections.shuffle(availableRows);
        int index = 0;
        for (Integer row : availableRows) {
            annotations.clear();
            annotations.addAll(getFirstInputSlot().getTextAnnotations(row));
            annotations.add(new JIPipeTextAnnotation(generatedAnnotation, "" + (index++)));
            getFirstOutputSlot().addData(getFirstInputSlot().getDataItemStore(row),
                    annotations,
                    annotationMergeStrategy,
                    getFirstInputSlot().getDataAnnotations(row),
                    JIPipeDataAnnotationMergeMode.OverwriteExisting);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        report.resolve("Generated annotation").checkNonEmpty(generatedAnnotation, this);
    }

    /**
     * @return Generated annotation type
     */
    @JIPipeDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each data row")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets generated annotation type
     *
     * @param generatedAnnotation Annotation type
     */
    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }


    @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if an annotation already exists.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
