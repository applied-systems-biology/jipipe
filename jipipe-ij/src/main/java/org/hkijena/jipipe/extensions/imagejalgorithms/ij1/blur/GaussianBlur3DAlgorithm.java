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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link GaussianBlur}
 */
@JIPipeDocumentation(name = "Gaussian blur 3D", description = "Applies convolution with a Gaussian function in 3D space for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 3D slice.")
@JIPipeOrganization(menuPath = "Blur", algorithmCategory = JIPipeAlgorithmCategory.Processor)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class GaussianBlur3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double sigmaX = 1;
    private double sigmaY = -1;
    private double sigmaZ = -1;

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public GaussianBlur3DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Sigma (X)").checkIfWithin(this, sigmaX, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @JIPipeDocumentation(name = "Sigma (X)", description = "Standard deviation of the Gaussian (pixels) in X direction. ")
    @JIPipeParameter("sigma-x")
    public double getSigmaX() {
        return sigmaX;
    }

    @JIPipeParameter("sigma-x")
    public void setSigmaX(double sigmaX) {
        this.sigmaX = sigmaX;

    }

    @JIPipeDocumentation(name = "Sigma (Y)", description = "Standard deviation of the Gaussian (pixels) in Y direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @JIPipeParameter("sigma-y")
    public double getSigmaY() {
        return sigmaY;
    }

    @JIPipeParameter("sigma-y")
    public void setSigmaY(double sigmaY) {
        this.sigmaY = sigmaY;

    }

    @JIPipeDocumentation(name = "Sigma (Z)", description = "Standard deviation of the Gaussian (pixels) in Z direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @JIPipeParameter("sigma-z")
    public double getSigmaZ() {
        return sigmaZ;
    }

    @JIPipeParameter("sigma-z")
    public void setSigmaZ(double sigmaZ) {
        this.sigmaZ = sigmaZ;

    }
}
