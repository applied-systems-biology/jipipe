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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.roi.IntModificationParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Scale 2D image", description = "Scales a 2D image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Transform", algorithmCategory = JIPipeAlgorithmCategory.Processor)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformScale2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private InterpolationMethod interpolationMethod = InterpolationMethod.Bilinear;
    private IntModificationParameter xAxis = new IntModificationParameter();
    private IntModificationParameter yAxis = new IntModificationParameter();
    private boolean useAveraging = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public TransformScale2DAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
    public void reportValidity(JIPipeValidityReport report) {
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

    @JIPipeDocumentation(name = "Interpolation", description = "The interpolation method")
    @JIPipeParameter("interpolation-method")
    public InterpolationMethod getInterpolationMethod() {
        return interpolationMethod;
    }

    @JIPipeParameter("interpolation-method")
    public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    @JIPipeDocumentation(name = "X axis", description = "How the X axis should be scaled")
    @JIPipeParameter("x-axis")
    public IntModificationParameter getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis")
    public void setxAxis(IntModificationParameter xAxis) {
        this.xAxis = xAxis;
    }

    @JIPipeDocumentation(name = "Y axis", description = "How the Y axis should be scaled")
    @JIPipeParameter("y-axis")
    public IntModificationParameter getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis")
    public void setyAxis(IntModificationParameter yAxis) {
        this.yAxis = yAxis;
    }

    @JIPipeDocumentation(name = "Use averaging", description = "True means that the averaging occurs to avoid " +
            "aliasing artifacts; the kernel shape for averaging is determined by " +
            "the interpolationMethod. False if subsampling without any averaging " +
            "should be used on downsizing. Has no effect on upsizing.")
    @JIPipeParameter("use-averaging")
    public boolean isUseAveraging() {
        return useAveraging;
    }

    @JIPipeParameter("use-averaging")
    public void setUseAveraging(boolean useAveraging) {
        this.useAveraging = useAveraging;
    }
}
