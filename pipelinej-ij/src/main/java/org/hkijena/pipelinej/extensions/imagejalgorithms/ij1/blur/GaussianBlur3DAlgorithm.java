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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.pipelinej.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link GaussianBlur}
 */
@ACAQDocumentation(name = "Gaussian blur 3D", description = "Applies convolution with a Gaussian function in 3D space for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 3D slice.")
@ACAQOrganization(menuPath = "Blur", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class GaussianBlur3DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double sigmaX = 1;
    private double sigmaY = -1;
    private double sigmaZ = -1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public GaussianBlur3DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public GaussianBlur3DAlgorithm(GaussianBlur3DAlgorithm other) {
        super(other);
        this.sigmaX = other.sigmaX;
        this.sigmaY = other.sigmaY;
        this.sigmaZ = other.sigmaZ;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        GaussianBlur3D.blur(img, sigmaX, sigmaY <= 0 ? sigmaX : sigmaY, sigmaZ <= 0 ? sigmaX : sigmaZ);
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Sigma (X)").checkIfWithin(this, sigmaX, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @ACAQDocumentation(name = "Sigma (X)", description = "Standard deviation of the Gaussian (pixels) in X direction. ")
    @ACAQParameter("sigma-x")
    public double getSigmaX() {
        return sigmaX;
    }

    @ACAQParameter("sigma-x")
    public void setSigmaX(double sigmaX) {
        this.sigmaX = sigmaX;

    }

    @ACAQDocumentation(name = "Sigma (Y)", description = "Standard deviation of the Gaussian (pixels) in Y direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @ACAQParameter("sigma-y")
    public double getSigmaY() {
        return sigmaY;
    }

    @ACAQParameter("sigma-y")
    public void setSigmaY(double sigmaY) {
        this.sigmaY = sigmaY;

    }

    @ACAQDocumentation(name = "Sigma (Z)", description = "Standard deviation of the Gaussian (pixels) in Z direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @ACAQParameter("sigma-z")
    public double getSigmaZ() {
        return sigmaZ;
    }

    @ACAQParameter("sigma-z")
    public void setSigmaZ(double sigmaZ) {
        this.sigmaZ = sigmaZ;

    }
}
