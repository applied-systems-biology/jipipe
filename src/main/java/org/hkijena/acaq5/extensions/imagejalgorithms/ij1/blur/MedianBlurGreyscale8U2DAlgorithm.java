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
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@ACAQDocumentation(name = "Median blur 3x3 2D (8-bit greyscale)", description = "Applies a 3x3 median filter to 8-bit greyscale images. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Blur", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Output")
public class MedianBlurGreyscale8U2DAlgorithm extends ACAQSimpleIteratingAlgorithm {


    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public MedianBlurGreyscale8U2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscale8UData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public MedianBlurGreyscale8U2DAlgorithm(MedianBlurGreyscale8U2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachSlice(img, ImageProcessor::medianFilter);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale8UData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
