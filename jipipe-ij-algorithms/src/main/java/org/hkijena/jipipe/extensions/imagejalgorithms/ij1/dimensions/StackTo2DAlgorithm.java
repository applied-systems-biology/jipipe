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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_2D_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Split stack into 2D", description = "Splits high-dimensional image stacks into 2D planes")
@JIPipeOrganization(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlus2DData.class, slotName = "Output")
public class StackTo2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean annotateSlices = true;
    private String annotationType = "Image index";

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StackTo2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlus2DData.class, "Input", TO_2D_CONVERSION)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StackTo2DAlgorithm(StackTo2DAlgorithm other) {
        super(other);
        this.annotateSlices = other.annotateSlices;
        this.annotationType = other.annotationType;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachIndexedSlice(img, (ip, index) -> {
            if (annotateSlices) {
                JIPipeAnnotation trait = new JIPipeAnnotation(annotationType, "slice=" + index);
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(new ImagePlus("slice=" + index, ip)), Collections.singletonList(trait), JIPipeAnnotationMergeStrategy.Merge);
            } else {
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(new ImagePlus("slice=" + index, ip)));
            }
        });
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (annotateSlices) {
            report.forCategory("Generated annotation").checkNonEmpty(annotationType, this);
        }
    }

    @JIPipeDocumentation(name = "Annotate slices", description = "Annotates each generated image slice.")
    @JIPipeParameter("annotate-slices")
    public boolean isAnnotateSlices() {
        return annotateSlices;
    }

    @JIPipeParameter("annotate-slices")
    public void setAnnotateSlices(boolean annotateSlices) {
        this.annotateSlices = annotateSlices;
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "Determines the generated annotation type.")
    @JIPipeParameter("annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getAnnotationType() {
        return annotationType;
    }

    @JIPipeParameter("annotation-type")
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }
}
