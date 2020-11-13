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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Converts ImageJ data type into each other
 */
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
@JIPipeDocumentation(name = "Convert ImageJ image", description = "Converts an ImageJ image into another ImageJ image data type")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class)
public class ImageTypeConverter extends JIPipeAlgorithm {

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public ImageTypeConverter(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ImagePlusData.class)
                .restrictOutputTo(ImageJDataTypesExtension.IMAGE_TYPES)
                .restrictOutputSlotCount(1)
                .sealInput()
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ImageTypeConverter(ImageTypeConverter other) {
        super(other);
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        JIPipeDataSlot inputSlot = getFirstInputSlot();
        JIPipeDataSlot outputSlot = getFirstOutputSlot();
        for (int i = 0; i < inputSlot.getRowCount(); ++i) {
            ImagePlusData data = inputSlot.getData(i, ImagePlusData.class);
            JIPipeData converted = JIPipe.createData(outputSlot.getAcceptedDataType(), data.getImage());
            outputSlot.addData(converted, outputSlot.getAnnotations(i), JIPipeAnnotationMergeStrategy.Merge);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (getInputSlots().isEmpty()) {
            report.reportIsInvalid("No input slot!",
                    "Please add an input slot that provides the data that should be converted.",
                    "Please provide an input image slot.",
                    this);
        }
        if (getOutputSlots().isEmpty()) {
            report.reportIsInvalid("No output slot!",
                    "The converted image is stored into the output slot.",
                    "Please provide an output image slot.",
                    this);
        }
        if (!getInputSlots().isEmpty() && !getOutputSlots().isEmpty()) {
            int inputDimensionality = ImagePlusData.getDimensionalityOf((Class<? extends ImagePlusData>) getFirstInputSlot().getAcceptedDataType());
            int outputDimensionality = ImagePlusData.getDimensionalityOf((Class<? extends ImagePlusData>) getFirstOutputSlot().getAcceptedDataType());
            if (inputDimensionality != -1 && outputDimensionality != -1) {
                if (outputDimensionality < inputDimensionality) {
                    report.reportIsInvalid("Invalid conversion", "Non-trivial conversion between image dimensions: From " + inputDimensionality + "D to "
                                    + outputDimensionality + "D!", "Update the slots, so inter-dimensional conversion is trivial.",
                            this);
                }
            }
        }
    }
}
