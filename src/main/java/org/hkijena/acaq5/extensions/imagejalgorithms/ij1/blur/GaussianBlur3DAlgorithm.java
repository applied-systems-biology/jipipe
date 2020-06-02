package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link GaussianBlur}
 */
@ACAQDocumentation(name = "Gaussian blur 3D", description = "Applies convolution with a Gaussian function in 3D space for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 3D slice.")
@ACAQOrganization(menuPath = "Blur", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class GaussianBlur3DAlgorithm extends ImageJ1Algorithm {

    private double sigmaX = 1;
    private double sigmaY = -1;
    private double sigmaZ = -1;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public GaussianBlur3DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
        getEventBus().post(new ParameterChangedEvent(this, "sigma-x"));
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
        getEventBus().post(new ParameterChangedEvent(this, "sigma-y"));
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
        getEventBus().post(new ParameterChangedEvent(this, "sigma-z"));
    }
}
