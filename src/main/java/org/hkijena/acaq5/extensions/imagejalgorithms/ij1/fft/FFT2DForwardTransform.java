package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.fft;

import ij.ImagePlus;
import ij.plugin.FFT;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft.ImagePlusFFT2DData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies a FFT forward transformation
 */
@ACAQDocumentation(name = "2D FFT forward transformation", description = "Converts a real space image into an image in frequency space.")
@ACAQOrganization(menuPath = "FFT", algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(ImagePlus2DData.class)
@AlgorithmOutputSlot(ImagePlusFFT2DData.class)
public class FFT2DForwardTransform extends ImageJ1Algorithm {

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public FFT2DForwardTransform(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlus2DData.class)
                .addOutputSlot("Output", ImagePlusFFT2DData.class, null)
                .allowOutputSlotInheritance(false)
                .seal()
                .build());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FFT2DForwardTransform(ImageJ1Algorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlus2DData.class).getImage();
        ImagePlus fft = FFT.forward(img);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusFFT2DData(fft));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
