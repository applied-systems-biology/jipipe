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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.fft;

import ij.ImagePlus;
import ij.plugin.FFT;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
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
public class FFT2DInverseTransform extends ACAQSimpleIteratingAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration the algorithm declaration
     */
    public FFT2DInverseTransform(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusFFT2DData.class)
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
    public FFT2DInverseTransform(FFT2DInverseTransform other) {
        super(other);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
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
