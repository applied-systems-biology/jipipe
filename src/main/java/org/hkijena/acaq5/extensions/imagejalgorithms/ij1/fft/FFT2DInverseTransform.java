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
@ACAQDocumentation(name = "2D FFT inverse transformation", description = "Converts a frequency space image into an image in real space.")
@ACAQOrganization(menuPath = "FFT", algorithmCategory = ACAQAlgorithmCategory.Converter)
@AlgorithmInputSlot(ImagePlusFFT2DData.class)
@AlgorithmOutputSlot(ImagePlus2DData.class)
public class FFT2DInverseTransform extends ImageJ1Algorithm {

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public FFT2DInverseTransform(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusFFT2DData.class)
                .addOutputSlot("Output", ImagePlus2DData.class, null)
                .allowOutputSlotInheritance(false)
                .seal()
                .build());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FFT2DInverseTransform(ImageJ1Algorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlusFFT2DData.class).getImage();
        ImagePlus fft = FFT.inverse(img);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DData(fft));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
