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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.background;

import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.RankFilters;
import ij.process.ColorProcessor;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;


/**
 * Wrapper around {@link RankFilters}
 */
@SetJIPipeDocumentation(name = "Find or subtract background 2D", description = "Finds or subtracts the background of a 2D slice via a rolling-ball or paraboloid algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Background", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Subtract Background...")
public class RollingBallBackgroundEstimator2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double radius = 50;
    private boolean subtract = true;
    private BackgroundType backgroundType = BackgroundType.DarkBackground;
    private Method method = Method.RollingBall;
    private boolean preSmoothing = false;
    private boolean correctCorners = true;
    private boolean separateChannels = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RollingBallBackgroundEstimator2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RollingBallBackgroundEstimator2DAlgorithm(RollingBallBackgroundEstimator2DAlgorithm other) {
        super(other);
        this.radius = other.radius;
        this.backgroundType = other.backgroundType;
        this.correctCorners = other.correctCorners;
        this.method = other.method;
        this.preSmoothing = other.preSmoothing;
        this.separateChannels = other.separateChannels;
        this.subtract = other.subtract;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        ImageJIterationUtils.forEachSlice(img, ip -> {
            if (ip instanceof ColorProcessor && !separateChannels) {
                backgroundSubtracter.rollingBallBrightnessBackground((ColorProcessor) ip,
                        radius,
                        !subtract,
                        backgroundType == BackgroundType.LightBackground,
                        method == Method.RollingParaboloid,
                        preSmoothing,
                        correctCorners);
            } else {
                backgroundSubtracter.rollingBallBackground(ip,
                        radius,
                        !subtract,
                        backgroundType == BackgroundType.LightBackground,
                        method == Method.RollingParaboloid,
                        preSmoothing,
                        correctCorners);
            }
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Radius", description = "Rolling ball / Rolling paraboloid radius in pixels")
    @JIPipeParameter("radius")
    public double getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public boolean setRadius(double radius) {
        if (radius <= 0) {
            return false;
        }
        this.radius = radius;
        return true;
    }

    @SetJIPipeDocumentation(name = "Subtract background", description = "If enabled, the background is subtracted. If disabled, the background is returned as output.")
    @JIPipeParameter("subtract")
    public boolean isSubtract() {
        return subtract;
    }

    @JIPipeParameter("subtract")
    public void setSubtract(boolean subtract) {
        this.subtract = subtract;

    }

    @SetJIPipeDocumentation(name = "Background type", description = "Determines whether the background is dark or light.")
    @JIPipeParameter("background-type")
    public BackgroundType getBackgroundType() {
        return backgroundType;
    }

    @JIPipeParameter("background-type")
    public void setBackgroundType(BackgroundType backgroundType) {
        this.backgroundType = backgroundType;

    }

    @SetJIPipeDocumentation(name = "Method", description = "The method to estimate the background")
    @JIPipeParameter("method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
    }

    @SetJIPipeDocumentation(name = "Apply pre-smoothing", description = "If enabled, a 3x3 box-filter is applied before extracting the background. " +
            "Please note that this might lead to the background not necessary being lower than the foreground.")
    @JIPipeParameter("apply-pre-smoothing")
    public boolean isPreSmoothing() {
        return preSmoothing;
    }

    @JIPipeParameter("apply-pre-smoothing")
    public void setPreSmoothing(boolean preSmoothing) {
        this.preSmoothing = preSmoothing;

    }

    @SetJIPipeDocumentation(name = "Apply corner-correction", description = "If enabled, the algorithm attempts to avoid recognizing corners as background.")
    @JIPipeParameter("apply-corner-correction")
    public boolean isCorrectCorners() {
        return correctCorners;
    }

    @JIPipeParameter("apply-corner-correction")
    public void setCorrectCorners(boolean correctCorners) {
        this.correctCorners = correctCorners;

    }

    @SetJIPipeDocumentation(name = "Separate channels", description = "Only valid for multi-channel images. If enabled, the algorithm is applied to each channel individually. " +
            "If disabled, the background is calculated based on the HSV brightness value. The hue is unaffected.")
    @JIPipeParameter("separate-channels")
    public boolean isSeparateChannels() {
        return separateChannels;
    }

    @JIPipeParameter("separate-channels")
    public void setSeparateChannels(boolean separateChannels) {
        this.separateChannels = separateChannels;

    }

    /**
     * Available background types
     */
    public enum BackgroundType {
        LightBackground,
        DarkBackground
    }

    /**
     * Available methods
     */
    public enum Method {
        RollingBall,
        RollingParaboloid
    }
}
