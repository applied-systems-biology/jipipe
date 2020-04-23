package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Manual threshold (16-bit)", description = "Thresholds the image with a manual threshold")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Segmentation)
@AlgorithmInputSlot(value = ImagePlusGreyscale16UData.class, slotName = "Input")
@AlgorithmInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class ManualThreshold16UAlgorithm extends ImageJ1Algorithm {

    private int threshold = 0;

    /**
     * Instantiates a new Gaussian blur algorithm.
     *
     * @param declaration the declaration
     */
    public ManualThreshold16UAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale16UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new Gaussian blur algorithm.
     *
     * @param other the other
     */
    public ManualThreshold16UAlgorithm(ManualThreshold16UAlgorithm other) {
        super(other);
        this.threshold = other.threshold;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachSlice(img, ip -> ip.threshold(threshold));
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Threshold").checkIfWithin(threshold, 0, 2 * Short.MAX_VALUE, true, true);
    }

    @ACAQDocumentation(name = "Threshold", description = "All pixel values less or equal to this are set to zero. The value interval is [0, " + 2 * Short.MAX_VALUE + "]")
    @ACAQParameter("threshold")
    public int getThreshold() {
        return threshold;
    }

    @ACAQParameter("threshold")
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
