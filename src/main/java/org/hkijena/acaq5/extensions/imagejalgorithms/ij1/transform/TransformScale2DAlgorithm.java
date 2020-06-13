package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.parameters.roi.IntModificationParameter;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Scale 2D image", description = "Scales a 2D image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Transform", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformScale2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private InterpolationMethod interpolationMethod = InterpolationMethod.Bilinear;
    private IntModificationParameter xAxis = new IntModificationParameter();
    private IntModificationParameter yAxis = new IntModificationParameter();
    private boolean useAveraging = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public TransformScale2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
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
    public TransformScale2DAlgorithm(TransformScale2DAlgorithm other) {
        super(other);
        this.interpolationMethod = other.interpolationMethod;
        this.xAxis = new IntModificationParameter(other.xAxis);
        this.yAxis = new IntModificationParameter(other.yAxis);
        this.useAveraging = other.useAveraging;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage();

        final int sx = xAxis.apply(img.getWidth());
        final int sy = yAxis.apply(img.getHeight());

        if (img.isStack()) {
            ImageStack result = new ImageStack(sx, sy, img.getProcessor().getColorModel());
            ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
                imp.setInterpolationMethod(interpolationMethod.getNativeValue());
                ImageProcessor resized = img.getProcessor().resize(sx, sy, useAveraging);
                result.addSlice("" + index, resized);
            });
            dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Resized", result)));
        } else {
            img.getProcessor().setInterpolationMethod(interpolationMethod.getNativeValue());
            ImageProcessor resized = img.getProcessor().resize(sx, sy, useAveraging);
            ImagePlus result = new ImagePlus("Resized", resized);
            dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
        }

    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (xAxis.isUseExactValue()) {
            report.forCategory("X axis").checkIfWithin(this, xAxis.getExactValue(), 0, Double.POSITIVE_INFINITY, false, false);
        } else {
            report.forCategory("X axis").checkIfWithin(this, xAxis.getFactor(), 0, Double.POSITIVE_INFINITY, false, false);
        }
        if (yAxis.isUseExactValue()) {
            report.forCategory("Y axis").checkIfWithin(this, yAxis.getExactValue(), 0, Double.POSITIVE_INFINITY, false, false);
        } else {
            report.forCategory("Y axis").checkIfWithin(this, yAxis.getFactor(), 0, Double.POSITIVE_INFINITY, false, false);
        }
    }

    @ACAQDocumentation(name = "Interpolation", description = "The interpolation method")
    @ACAQParameter("interpolation-method")
    public InterpolationMethod getInterpolationMethod() {
        return interpolationMethod;
    }

    @ACAQParameter("interpolation-method")
    public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    @ACAQDocumentation(name = "X axis", description = "How the X axis should be scaled")
    @ACAQParameter("x-axis")
    public IntModificationParameter getxAxis() {
        return xAxis;
    }

    @ACAQParameter("x-axis")
    public void setxAxis(IntModificationParameter xAxis) {
        this.xAxis = xAxis;
    }

    @ACAQDocumentation(name = "Y axis", description = "How the Y axis should be scaled")
    @ACAQParameter("y-axis")
    public IntModificationParameter getyAxis() {
        return yAxis;
    }

    @ACAQParameter("y-axis")
    public void setyAxis(IntModificationParameter yAxis) {
        this.yAxis = yAxis;
    }

    @ACAQDocumentation(name = "Use averaging", description = "True means that the averaging occurs to avoid " +
            "aliasing artifacts; the kernel shape for averaging is determined by " +
            "the interpolationMethod. False if subsampling without any averaging " +
            "should be used on downsizing. Has no effect on upsizing.")
    @ACAQParameter("use-averaging")
    public boolean isUseAveraging() {
        return useAveraging;
    }

    @ACAQParameter("use-averaging")
    public void setUseAveraging(boolean useAveraging) {
        this.useAveraging = useAveraging;
    }
}
