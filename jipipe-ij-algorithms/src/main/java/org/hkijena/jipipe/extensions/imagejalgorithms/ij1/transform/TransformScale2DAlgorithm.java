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
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.roi.OptionalIntModificationParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Scale 2D image", description = "Scales a 2D image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class TransformScale2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private InterpolationMethod interpolationMethod = InterpolationMethod.Bilinear;
    private OptionalIntModificationParameter xAxis = new OptionalIntModificationParameter();
    private OptionalIntModificationParameter yAxis = new OptionalIntModificationParameter();
    private boolean useAveraging = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformScale2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input")
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        xAxis.setEnabled(true);
        yAxis.setEnabled(true);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public TransformScale2DAlgorithm(TransformScale2DAlgorithm other) {
        super(other);
        this.interpolationMethod = other.interpolationMethod;
        this.xAxis = new OptionalIntModificationParameter(other.xAxis);
        this.yAxis = new OptionalIntModificationParameter(other.yAxis);
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

        int sx = img.getWidth();
        int sy = img.getHeight();
        if (xAxis.isEnabled() && yAxis.isEnabled()) {
            sx = xAxis.getContent().apply(sx);
            sy = yAxis.getContent().apply(sy);
        } else if (xAxis.isEnabled()) {
            sx = xAxis.getContent().apply(sx);
            double fac = (double) sx / img.getWidth();
            sy = (int) (sy * fac);
        } else if (yAxis.isEnabled()) {
            sy = yAxis.getContent().apply(sy);
            double fac = (double) sy / img.getHeight();
            sx = (int) (sx * fac);
        }

        if (img.isStack()) {
            ImageStack result = new ImageStack(sx, sy, img.getProcessor().getColorModel());
            int finalSx = sx;
            int finalSy = sy;
            ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
                imp.setInterpolationMethod(interpolationMethod.getNativeValue());
                ImageProcessor resized = img.getProcessor().resize(finalSx, finalSy, useAveraging);
                result.addSlice("" + index, resized);
            });
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Resized", result)));
        } else {
            img.getProcessor().setInterpolationMethod(interpolationMethod.getNativeValue());
            ImageProcessor resized = img.getProcessor().resize(sx, sy, useAveraging);
            ImagePlus result = new ImagePlus("Resized", resized);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
        }

    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (xAxis.isEnabled() && xAxis.getContent().isUseExactValue()) {
            report.forCategory("X axis").checkIfWithin(this, xAxis.getContent().getExactValue(), 0, Double.POSITIVE_INFINITY, false, false);
        } else {
            report.forCategory("X axis").checkIfWithin(this, xAxis.getContent().getFactor(), 0, Double.POSITIVE_INFINITY, false, false);
        }
        if (yAxis.isEnabled() && yAxis.getContent().isUseExactValue()) {
            report.forCategory("Y axis").checkIfWithin(this, yAxis.getContent().getExactValue(), 0, Double.POSITIVE_INFINITY, false, false);
        } else {
            report.forCategory("Y axis").checkIfWithin(this, yAxis.getContent().getFactor(), 0, Double.POSITIVE_INFINITY, false, false);
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

    @JIPipeDocumentation(name = "X axis", description = "How the X axis should be scaled. If disabled, the aspect ratio is kept.")
    @JIPipeParameter("x-axis")
    public OptionalIntModificationParameter getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis")
    public void setxAxis(OptionalIntModificationParameter xAxis) {
        this.xAxis = xAxis;
    }

    @JIPipeDocumentation(name = "Y axis", description = "How the Y axis should be scaled. If disabled, the aspect ratio is kept.")
    @JIPipeParameter("y-axis")
    public OptionalIntModificationParameter getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis")
    public void setyAxis(OptionalIntModificationParameter yAxis) {
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
