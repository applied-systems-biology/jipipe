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

package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FolderData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * Algorithm that generates annotations from folder names
 */
@JIPipeDocumentation(name = "Folders to annotations", description = "Creates an annotation for each path based on its name")
@JIPipeOrganization(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "Generate")
@JIPipeInputSlot(value = PathData.class, slotName = "Folders", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Annotated folders", autoCreate = true)
@JIPipeHidden
public class SimpleFolderAnnotationGenerator extends JIPipeSimpleIteratingAlgorithm {

    private String generatedAnnotation = "";

    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public SimpleFolderAnnotationGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public SimpleFolderAnnotationGenerator(SimpleFolderAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            FolderData inputData = dataBatch.getInputData(getFirstInputSlot(), FolderData.class, progressInfo);
            String discriminator = inputData.toPath().getFileName().toString();
            dataBatch.addGlobalAnnotation(new JIPipeAnnotation(generatedAnnotation, discriminator), JIPipeAnnotationMergeStrategy.OverwriteExisting);
            dataBatch.addOutputData(getFirstOutputSlot(), inputData, progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Generated annotation").checkNonEmpty(generatedAnnotation, this);
    }

    /**
     * @return The generated annotation type
     */
    @JIPipeDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each folder")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    /**
     * Sets the generated annotation type
     *
     * @param generatedAnnotation The annotation type
     */
    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }
}
