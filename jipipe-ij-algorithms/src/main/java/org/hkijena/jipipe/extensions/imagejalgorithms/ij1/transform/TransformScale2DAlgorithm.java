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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.InterpolationMethod;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.roi.OptionalIntModificationParameter;

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
    private ScaleMode scaleMode = ScaleMode.Stretch;

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
        this.scaleMode = other.scaleMode;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getImage();

        int sx = img.getWidth();
        int sy = img.getHeight();
        if (xAxis.isEnabled() && yAxis.isEnabled()) {
            sx = (int) xAxis.getContent().apply(sx);
            sy = (int) yAxis.getContent().apply(sy);
        } else if (xAxis.isEnabled()) {
            sx = (int) xAxis.getContent().apply(sx);
            double fac = (double) sx / img.getWidth();
            sy = (int) (sy * fac);
        } else if (yAxis.isEnabled()) {
            sy = (int) yAxis.getContent().apply(sy);
            double fac = (double) sy / img.getHeight();
            sx = (int) (sx * fac);
        }

        if (img.isStack()) {
            ImageStack result = new ImageStack(sx, sy, img.getProcessor().getColorModel());
            int finalSx = sx;
            int finalSy = sy;
            ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
                ImageProcessor resized = scaleProcessor(imp, finalSx, finalSy, interpolationMethod, useAveraging, scaleMode);
                result.addSlice("" + index, resized);
            }, progressInfo);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(new ImagePlus("Resized", result)), progressInfo);
        } else {
            ImageProcessor resized = scaleProcessor(img.getProcessor(), sx, sy, interpolationMethod, useAveraging, scaleMode);
            ImagePlus result = new ImagePlus("Resized", resized);
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
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

    @JIPipeDocumentation(name = "Scale mode", description = "Determines how the image is fit into the output. You can either stretch the image " +
            "to the new dimensions, fit it inside the boundaries, or cut off parts to cover the whole output")
    @JIPipeParameter("scale-mode")
    public ScaleMode getScaleMode() {
        return scaleMode;
    }

    @JIPipeParameter("scale-mode")
    public void setScaleMode(ScaleMode scaleMode) {
        this.scaleMode = scaleMode;
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

    public static ImageProcessor scaleProcessor(ImageProcessor imp, int width, int height, InterpolationMethod interpolationMethod, boolean useAveraging, ScaleMode scaleMode) {
        imp.setInterpolationMethod(interpolationMethod.getNativeValue());
        switch (scaleMode) {
            case Stretch:
                return imp.resize(width, height, useAveraging);
            case Fit: {
                double factor = Math.min(width * 1.0 / imp.getWidth(), height * 1.0 / imp.getHeight());
                ImageProcessor resized = imp.resize((int) (imp.getWidth() * factor), (int) (imp.getHeight() * factor), useAveraging);
                ImageProcessor container = IJ.createImage("", width, height, 1, imp.getBitDepth()).getProcessor();
                container.insert(resized, width / 2 - resized.getWidth() / 2, height / 2 - resized.getHeight() / 2);
                return container;
            }
            case Cover: {
                double factor = Math.max(width * 1.0 / imp.getWidth(), height * 1.0 / imp.getHeight());
                ImageProcessor resized = imp.resize((int) (imp.getWidth() * factor), (int) (imp.getHeight() * factor), useAveraging);
                ImageProcessor container = IJ.createImage("", width, height, 1, imp.getBitDepth()).getProcessor();
                container.insert(resized, width / 2 - resized.getWidth() / 2, height / 2 - resized.getHeight() / 2);
                return container;
            }
            default:
                throw new UnsupportedOperationException();
        }
    }
}
