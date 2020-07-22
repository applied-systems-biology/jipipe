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
import ij.plugin.Resizer;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.roi.IntModificationParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Scale 3D image", description = "Scales a 3D image.")
@JIPipeOrganization(menuPath = "Transform", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformScale3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private InterpolationMethod interpolationMethod = InterpolationMethod.Bilinear;
    private IntModificationParameter xAxis = new IntModificationParameter();
    private IntModificationParameter yAxis = new IntModificationParameter();
    private IntModificationParameter zAxis = new IntModificationParameter();
    private boolean useAveraging = true;
    private TransformScale2DAlgorithm scale2DAlgorithm = JIPipeAlgorithm.newInstance("ij1-transform-scale2d");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformScale3DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class);
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

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
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
        if (zAxis.isUseExactValue()) {
            report.forCategory("Z axis").checkIfWithin(this, zAxis.getExactValue(), 0, Double.POSITIVE_INFINITY, false, false);
        } else {
            report.forCategory("Z axis").checkIfWithin(this, zAxis.getFactor(), 0, Double.POSITIVE_INFINITY, false, false);
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

    @JIPipeDocumentation(name = "Z axis", description = "How the Z axis should be scaled")
    @JIPipeParameter("z-axis")
    public IntModificationParameter getzAxis() {
        return zAxis;
    }

    @JIPipeParameter("z-axis")
    public void setzAxis(IntModificationParameter zAxis) {
        this.zAxis = zAxis;
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
