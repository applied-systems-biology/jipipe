/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.InterpolationMethod;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions.ImagePlusPropertiesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;

import java.awt.*;

/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Scale 2D image", description = "Scales a 2D image. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Transform", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image", aliasName = "Scale... (2D)")
public class TransformScale2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private InterpolationMethod interpolationMethod = InterpolationMethod.Bilinear;
    private OptionalJIPipeExpressionParameter xAxis = new OptionalJIPipeExpressionParameter(true, "width");
    private OptionalJIPipeExpressionParameter yAxis = new OptionalJIPipeExpressionParameter(true, "height");
    private ScaleMode scaleMode = ScaleMode.Stretch;
    private Anchor anchor = Anchor.CenterCenter;
    private Color background = Color.BLACK;
    private boolean avoidUnnecessaryScaling = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TransformScale2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
        this.xAxis = new OptionalJIPipeExpressionParameter(other.xAxis);
        this.yAxis = new OptionalJIPipeExpressionParameter(other.yAxis);
        this.scaleMode = other.scaleMode;
        this.anchor = other.anchor;
        this.background = other.background;
        this.avoidUnnecessaryScaling = other.avoidUnnecessaryScaling;
    }

    public static ImageProcessor scaleProcessor(ImageProcessor imp, int width, int height, InterpolationMethod interpolationMethod, boolean useAveraging, ScaleMode scaleMode, Anchor location, Color background) {
        imp.setInterpolationMethod(interpolationMethod.getNativeValue());

        switch (scaleMode) {
            case Stretch:
                return imp.resize(width, height, useAveraging);
            case Fit: {
                double factor = Math.min(width * 1.0 / imp.getWidth(), height * 1.0 / imp.getHeight());
                ImageProcessor resized = imp.resize((int) (imp.getWidth() * factor), (int) (imp.getHeight() * factor), useAveraging);
                ImageProcessor container = IJ.createImage("", width, height, 1, imp.getBitDepth()).getProcessor();
                container.setRoi(0, 0, width, height);
                container.setColor(background);
                container.fill();
                container.setRoi((Roi) null);

                Rectangle finalRect = location.placeInside(new Rectangle(0, 0, resized.getWidth(), resized.getHeight()), new Rectangle(0, 0, width, height));
                container.insert(resized, finalRect.x, finalRect.y);
                return container;
            }
            case Paste: {
                ImageProcessor container = IJ.createImage("", width, height, 1, imp.getBitDepth()).getProcessor();
                container.setRoi(0, 0, width, height);
                container.setColor(background);
                container.fill();
                container.setRoi((Roi) null);

                Rectangle finalRect = location.placeInside(new Rectangle(0, 0, imp.getWidth(), imp.getHeight()), new Rectangle(0, 0, width, height));
                container.insert(imp, finalRect.x, finalRect.y);
                return container;
            }
            case Cover: {
                double factor = Math.max(width * 1.0 / imp.getWidth(), height * 1.0 / imp.getHeight());
                ImageProcessor resized = imp.resize((int) (imp.getWidth() * factor), (int) (imp.getHeight() * factor), useAveraging);
                ImageProcessor container = IJ.createImage("", width, height, 1, imp.getBitDepth()).getProcessor();
                container.setRoi(0, 0, width, height);
                container.setColor(background);
                container.fill();
                container.setRoi((Roi) null);

                Rectangle finalRect = location.placeInside(new Rectangle(0, 0, resized.getWidth(), resized.getHeight()), new Rectangle(0, 0, width, height));
                container.insert(resized, finalRect.x, finalRect.y);
                return container;
            }
            default:
                throw new UnsupportedOperationException();
        }
    }

    @SetJIPipeDocumentation(name = "Avoid unnecessary scaling", description = "If enabled, the ImageJ resize method is not called if the image already has the correct size.")
    @JIPipeParameter("avoid-unnecessary-scaling")
    public boolean isAvoidUnnecessaryScaling() {
        return avoidUnnecessaryScaling;
    }

    @JIPipeParameter("avoid-unnecessary-scaling")
    public void setAvoidUnnecessaryScaling(boolean avoidUnnecessaryScaling) {
        this.avoidUnnecessaryScaling = avoidUnnecessaryScaling;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getImage();

        int sx = img.getWidth();
        int sy = img.getHeight();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);
        ImagePlusPropertiesExpressionParameterVariablesInfo.extractValues(variables, img, iterationStep.getMergedTextAnnotations().values());

        if (xAxis.isEnabled() && yAxis.isEnabled()) {
            variables.set("x", sx);
            sx = (int) xAxis.getContent().evaluateToNumber(variables);
            variables.set("x", sy);
            sy = (int) yAxis.getContent().evaluateToNumber(variables);
        } else if (xAxis.isEnabled()) {
            variables.set("x", sx);
            sx = (int) xAxis.getContent().evaluateToNumber(variables);
            double fac = (double) sx / img.getWidth();
            sy = (int) (sy * fac);
        } else if (yAxis.isEnabled()) {
            variables.set("x", sy);
            sy = (int) yAxis.getContent().evaluateToNumber(variables);
            double fac = (double) sy / img.getHeight();
            sx = (int) (sx * fac);
        }

        if (avoidUnnecessaryScaling && img.getWidth() == sx && img.getHeight() == sy) {
            progressInfo.log("Image already has the target size. No scaling needed.");
            iterationStep.addOutputData(getFirstOutputSlot(), inputData, progressInfo);
        } else {
            if (img.hasImageStack()) {
                ImageStack result = new ImageStack(sx, sy, img.getStackSize());
                int finalSx = sx;
                int finalSy = sy;
                ImageJIterationUtils.forEachIndexedZCTSlice(img, (imp, index) -> {
                    ImageProcessor resized = scaleProcessor(imp, finalSx, finalSy, interpolationMethod, interpolationMethod != InterpolationMethod.None, scaleMode, anchor, background);
                    result.setProcessor(resized, index.zeroSliceIndexToOneStackIndex(img));
                }, progressInfo);
                ImagePlusData resized = new ImagePlusData(new ImagePlus("Resized", result));
                resized.getImage().setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
                resized.getImage().copyScale(img);
                iterationStep.addOutputData(getFirstOutputSlot(), resized, progressInfo);
            } else {
                ImageProcessor resized = scaleProcessor(img.getProcessor(), sx, sy, interpolationMethod, interpolationMethod != InterpolationMethod.None, scaleMode, anchor, background);
                ImagePlus result = new ImagePlus("Resized", resized);
                result.copyScale(img);
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Placement", description = "Used if the scale mode is 'Fit'/'Cover'/'Paste'. Determines where the image is placed.")
    @JIPipeParameter("anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @JIPipeParameter("anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    @SetJIPipeDocumentation(name = "Background", description = "Used if the scale mode is 'Fit'/'Cover'/'Paste'. Determines the background color of the output")
    @JIPipeParameter("background-color")
    public Color getBackground() {
        return background;
    }

    @JIPipeParameter("background-color")
    public void setBackground(Color background) {
        this.background = background;
    }

    @SetJIPipeDocumentation(name = "Interpolation", description = "The interpolation method")
    @JIPipeParameter("interpolation-method")
    public InterpolationMethod getInterpolationMethod() {
        return interpolationMethod;
    }

    @JIPipeParameter("interpolation-method")
    public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    @SetJIPipeDocumentation(name = "X axis", description = "How the X axis should be scaled. If disabled, the aspect ratio is kept.")
    @JIPipeParameter("x-axis")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis")
    public void setxAxis(OptionalJIPipeExpressionParameter xAxis) {
        this.xAxis = xAxis;
    }

    @SetJIPipeDocumentation(name = "Y axis", description = "How the Y axis should be scaled. If disabled, the aspect ratio is kept.")
    @JIPipeParameter("y-axis")
    @JIPipeExpressionParameterSettings(variableSource = ImagePlusPropertiesExpressionParameterVariablesInfo.class)
    public OptionalJIPipeExpressionParameter getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis")
    public void setyAxis(OptionalJIPipeExpressionParameter yAxis) {
        this.yAxis = yAxis;
    }

    @SetJIPipeDocumentation(name = "Scale mode", description = "Determines how the image is fit into the output. You can either stretch the image " +
            "to the new dimensions, fit it inside the boundaries, cut off parts to cover the whole output, or leave the image unchanged (no resize) and just copy it into the output image with a given size")
    @JIPipeParameter("scale-mode")
    public ScaleMode getScaleMode() {
        return scaleMode;
    }

    @JIPipeParameter("scale-mode")
    public void setScaleMode(ScaleMode scaleMode) {
        this.scaleMode = scaleMode;
    }
}
