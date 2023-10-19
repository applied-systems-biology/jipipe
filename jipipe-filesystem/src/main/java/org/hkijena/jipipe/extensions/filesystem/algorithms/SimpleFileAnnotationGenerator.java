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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * Generates annotations from filenames
 */
@JIPipeDocumentation(name = "Files to annotations", description = "Creates an annotation for each file based on its file name")
@JIPipeNode(nodeTypeCategory = AnnotationsNodeTypeCategory.class, menuPath = "For paths")
@JIPipeInputSlot(value = FileData.class, slotName = "Files", autoCreate = true)
@JIPipeOutputSlot(value = FileData.class, slotName = "Annotated files", autoCreate = true)
@JIPipeHidden
public class SimpleFileAnnotationGenerator extends JIPipeSimpleIteratingAlgorithm {

    private String generatedAnnotation = "#Dataset";

    /**
     * New instance
     *
     * @param info Algorithm info
     */
    public SimpleFileAnnotationGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other Original algorithm
     */
    public SimpleFileAnnotationGenerator(SimpleFileAnnotationGenerator other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
            FileData inputData = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo);
            String discriminator = inputData.toPath().getFileName().toString();
            iterationStep.addMergedTextAnnotation(new JIPipeTextAnnotation(generatedAnnotation, discriminator), JIPipeTextAnnotationMergeMode.OverwriteExisting);
            iterationStep.addOutputData(getFirstOutputSlot(), inputData, progressInfo);
        }
    }

    /**
     * @return Generated annotation type
     */
    @JIPipeDocumentation(name = "Generated annotation", description = "Select which annotation type is generated for each file")
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
}
