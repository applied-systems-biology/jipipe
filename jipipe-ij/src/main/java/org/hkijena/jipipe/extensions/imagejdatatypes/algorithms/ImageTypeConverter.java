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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Converts ImageJ data type into each other
 */
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
@JIPipeDocumentation(name = "Convert ImageJ image", description = "Converts an ImageJ image into another ImageJ image data type")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Converter)
public class ImageTypeConverter extends JIPipeAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ImageTypeConverter(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, createConfiguration());
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
            JIPipeData converted = JIPipeData.createInstance(outputSlot.getAcceptedDataType(), data.getImage());
            outputSlot.addData(converted, outputSlot.getAnnotations(i));
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

    /**
     * @return The appropriate slot configuration for {@link ImageTypeConverter}
     */
    public static JIPipeSlotConfiguration createConfiguration() {
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = new JIPipeDefaultMutableSlotConfiguration();
        slotConfiguration.setMaxInputSlots(1);
        slotConfiguration.setMaxOutputSlots(1);
        Set<Class<? extends JIPipeData>> allowedTypes = new HashSet<>();
        for (Class<? extends JIPipeData> type : JIPipeDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (ImagePlusData.class.isAssignableFrom(type)) {
                allowedTypes.add(type);
            }
        }
        slotConfiguration.setAllowedInputSlotTypes(allowedTypes);
        slotConfiguration.setAllowedOutputSlotTypes(allowedTypes);

        return slotConfiguration;
    }
}
