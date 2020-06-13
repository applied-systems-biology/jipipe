package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Median blur 2D (RGB color)", description = "Applies a 3x3 median filter to RGB color images. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Blur", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusColorRGBData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output")
public class MedianBlurRGB2DAlgorithm extends ACAQSimpleIteratingAlgorithm {


    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public MedianBlurRGB2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusColorRGBData.class)
                .addOutputSlot("Output", ImagePlusColorRGBData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public MedianBlurRGB2DAlgorithm(MedianBlurRGB2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusColorRGBData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachSlice(img, ImageProcessor::medianFilter);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusColorRGBData(img));
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
