package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.morphology;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.process.ImageProcessor}
 */
@ACAQDocumentation(name = "Morphological skeletonize", description = "Applies a morphological skeletonization operation to binary images." +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Morphology", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input")
@AlgorithmInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class MorphologySkeletonize2DAlgorithm extends ImageJ1Algorithm {

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public MorphologySkeletonize2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public MorphologySkeletonize2DAlgorithm(MorphologySkeletonize2DAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        ImageJUtils.forEachSlice(img, ip -> ((ByteProcessor) ip).skeletonize());
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
    }
}
