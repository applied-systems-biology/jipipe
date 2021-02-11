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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * Generates annotations from filenames
 */
@JIPipeDocumentation(name = "Annotate with data string", description = "Converts incoming data into its string representation and creates the a new annotation that " +
        "contains this generated string.")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Data", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Annotated data", inheritedSlot = "Data", autoCreate = true)
public class AnnotateWithDataString extends JIPipeSimpleIteratingAlgorithm {

    private String generatedAnnotation = "Data";
    private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public AnnotateWithDataString(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public AnnotateWithDataString(AnnotateWithDataString other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.annotationMergeStrategy = other.annotationMergeStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            JIPipeData inputData = dataBatch.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo);
            String discriminator = "" + inputData;
            dataBatch.addGlobalAnnotation(new JIPipeAnnotation(generatedAnnotation, discriminator), annotationMergeStrategy);
            dataBatch.addOutputData(getFirstOutputSlot(), inputData, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Generated annotation").checkNonEmpty(generatedAnnotation, this);
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
    public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
