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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
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
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.plugin.filter.GaussianBlur}
 */
@ACAQDocumentation(name = "Gaussian blur 2D", description = "Applies convolution with a Gaussian function for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Blur", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class GaussianBlur2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double sigmaX = 1;
    private double sigmaY = -1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public GaussianBlur2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
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
    public GaussianBlur2DAlgorithm(GaussianBlur2DAlgorithm other) {
        super(other);
        this.sigmaX = other.sigmaX;
        this.sigmaY = other.sigmaY;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        GaussianBlur gaussianBlur = new GaussianBlur();
        ImageJUtils.forEachSlice(img, ip -> {
            double accuracy = (ip instanceof ByteProcessor || ip instanceof ColorProcessor) ? 0.002 : 0.0002;
            gaussianBlur.blurGaussian(ip, sigmaX, sigmaY > 0 ? sigmaY : sigmaX, accuracy);
        });
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
}
