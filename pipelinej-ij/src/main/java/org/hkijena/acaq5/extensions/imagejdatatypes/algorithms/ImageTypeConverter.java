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

package org.hkijena.acaq5.extensions.imagejdatatypes.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Converts ImageJ data type into each other
 */
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
@ACAQDocumentation(name = "Convert ImageJ image", description = "Converts an ImageJ image into another ImageJ image data type")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Converter)
public class ImageTypeConverter extends ACAQAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ImageTypeConverter(ACAQAlgorithmDeclaration declaration) {
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
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ACAQDataSlot inputSlot = getFirstInputSlot();
        ACAQDataSlot outputSlot = getFirstOutputSlot();
        for (int i = 0; i < inputSlot.getRowCount(); ++i) {
            ImagePlusData data = inputSlot.getData(i, ImagePlusData.class);
            ACAQData converted = ACAQData.createInstance(outputSlot.getAcceptedDataType(), data.getImage());
            outputSlot.addData(converted, outputSlot.getAnnotations(i));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
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
    public static ACAQSlotConfiguration createConfiguration() {
        ACAQDefaultMutableSlotConfiguration slotConfiguration = new ACAQDefaultMutableSlotConfiguration();
        slotConfiguration.setMaxInputSlots(1);
        slotConfiguration.setMaxOutputSlots(1);
        Set<Class<? extends ACAQData>> allowedTypes = new HashSet<>();
        for (Class<? extends ACAQData> type : ACAQDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (ImagePlusData.class.isAssignableFrom(type)) {
                allowedTypes.add(type);
            }
        }
        slotConfiguration.setAllowedInputSlotTypes(allowedTypes);
        slotConfiguration.setAllowedOutputSlotTypes(allowedTypes);

        return slotConfiguration;
    }
}
