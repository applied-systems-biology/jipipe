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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.plugin.Resizer;
import ij.process.ImageProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.parameters.roi.IntModificationParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@ACAQDocumentation(name = "Scale 3D image", description = "Scales a 3D image.")
@ACAQOrganization(menuPath = "Transform", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformScale3DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private InterpolationMethod interpolationMethod = InterpolationMethod.Bilinear;
    private IntModificationParameter xAxis = new IntModificationParameter();
    private IntModificationParameter yAxis = new IntModificationParameter();
    private IntModificationParameter zAxis = new IntModificationParameter();
    private boolean useAveraging = true;
    private TransformScale2DAlgorithm scale2DAlgorithm = ACAQAlgorithm.newInstance("ij1-transform-scale2d");

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public TransformScale3DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
    public TransformScale3DAlgorithm(TransformScale3DAlgorithm other) {
        super(other);
        this.interpolationMethod = other.interpolationMethod;
        this.xAxis = new IntModificationParameter(other.xAxis);
        this.yAxis = new IntModificationParameter(other.yAxis);
        this.zAxis = new IntModificationParameter(other.zAxis);
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

        // Scale in 2D if needed
        final int sx = xAxis.apply(img.getWidth());
        final int sy = yAxis.apply(img.getHeight());
        if (sx != img.getWidth() || sy != img.getHeight()) {
            scale2DAlgorithm.clearSlotData();
            scale2DAlgorithm.setxAxis(xAxis);
            scale2DAlgorithm.setyAxis(yAxis);
            scale2DAlgorithm.getFirstInputSlot().addData(new ImagePlusData(img));
            scale2DAlgorithm.run(subProgress.resolve("Rescale 2D"), algorithmProgress, isCancelled);
            img = scale2DAlgorithm.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
        }

        // Scale in 3D
        final int sz = zAxis.apply(img.getStackSize());
        if (sz != img.getStackSize()) {
            Resizer resizer = new Resizer();
            img = resizer.zScale(img, sz, interpolationMethod.getNativeValue());
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
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
        if (zAxis.isUseExactValue()) {
            report.forCategory("Z axis").checkIfWithin(this, zAxis.getExactValue(), 0, Double.POSITIVE_INFINITY, false, false);
        } else {
            report.forCategory("Z axis").checkIfWithin(this, zAxis.getFactor(), 0, Double.POSITIVE_INFINITY, false, false);
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

    @ACAQDocumentation(name = "Z axis", description = "How the Z axis should be scaled")
    @ACAQParameter("z-axis")
    public IntModificationParameter getzAxis() {
        return zAxis;
    }

    @ACAQParameter("z-axis")
    public void setzAxis(IntModificationParameter zAxis) {
        this.zAxis = zAxis;
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
