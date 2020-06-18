package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.binary;

import ij.ImagePlus;
import ij.plugin.filter.EDM;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link EDM}
 */
@ACAQDocumentation(name = "Ultimate eroded points 2D", description = "Find the maxima of the Euclidean distance transform. In the output, the points " +
        "are assigned the EDM value, which is equal to the radius of the largest circle " +
        "that fits into the particle, with the UEP as the center. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Binary", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class UltimateErodedPoints2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public UltimateErodedPoints2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscaleMaskData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public UltimateErodedPoints2DAlgorithm(UltimateErodedPoints2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class);
        ImagePlus img = inputData.getImage().duplicate();
        EDM edm = new EDM();
        edm.setup("points", img);
        ImageJUtils.forEachSlice(img, edm::run);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale8UData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
