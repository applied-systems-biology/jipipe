package org.hkijena.acaq5.extensions.imagejdatatypes.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
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
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Converter)
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
            ImagePlusData data = inputSlot.getData(i);
            ACAQData converted = ACAQData.createInstance(outputSlot.getAcceptedDataType(), data.getImage());
            outputSlot.addData(converted, outputSlot.getAnnotations(i));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (getInputSlots().isEmpty()) {
            report.reportIsInvalid("No input slot! Please provide an input image slot.");
        }
        if (getOutputSlots().isEmpty()) {
            report.reportIsInvalid("No output slot! Please provide an output image slot.");
        }
        if (!getInputSlots().isEmpty() && !getOutputSlots().isEmpty()) {
            int inputDimensionality = ImagePlusData.getDimensionalityOf((Class<? extends ImagePlusData>) getFirstInputSlot().getAcceptedDataType());
            int outputDimensionality = ImagePlusData.getDimensionalityOf((Class<? extends ImagePlusData>) getFirstOutputSlot().getAcceptedDataType());
            if (inputDimensionality != -1 && outputDimensionality != -1) {
                if (outputDimensionality < inputDimensionality) {
                    report.reportIsInvalid("Non-trivial conversion between image dimensions: From " + inputDimensionality + "D to "
                            + outputDimensionality + "D! Please make sure to change the slots.");
                }
            }
        }
    }

    public static ACAQSlotConfiguration createConfiguration() {
        ACAQMutableSlotConfiguration slotConfiguration = new ACAQMutableSlotConfiguration();
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
