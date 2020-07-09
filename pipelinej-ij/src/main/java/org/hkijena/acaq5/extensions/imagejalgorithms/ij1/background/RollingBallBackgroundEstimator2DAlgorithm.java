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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.background;

import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.RankFilters;
import ij.process.ColorProcessor;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejalgorithms.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link RankFilters}
 */
@ACAQDocumentation(name = "Find or subtract background 2D", description = "Finds or subtracts the background of a 2D slice via a rolling-ball or paraboloid algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Background", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class RollingBallBackgroundEstimator2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double radius = 50;
    private boolean subtract = true;
    private BackgroundType backgroundType = BackgroundType.DarkBackground;
    private Method method = Method.RollingBall;
    private boolean preSmoothing = false;
    private boolean correctCorners = true;
    private boolean separateChannels = false;

    /**
     * Instantiates a new algorithm.
     *
     * @param declaration the declaration
     */
    public RollingBallBackgroundEstimator2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class);
        ImagePlus img = inputData.getImage().duplicate();
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        ImageJUtils.forEachSlice(img, ip -> {
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
        });
        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(img));
    }


    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Radius").checkIfWithin(this, radius, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @ACAQDocumentation(name = "Radius", description = "Rolling ball / Rolling paraboloid radius in pixels")
    @ACAQParameter("radius")
    public double getRadius() {
        return radius;
    }

    @ACAQParameter("radius")
    public boolean setRadius(double radius) {
        if (radius <= 0) {
            return false;
        }
        this.radius = radius;
        return true;
    }

    @ACAQDocumentation(name = "Subtract background", description = "If enabled, the background is subtracted. If disabled, the background is returned as output.")
    @ACAQParameter("subtract")
    public boolean isSubtract() {
        return subtract;
    }

    @ACAQParameter("subtract")
    public void setSubtract(boolean subtract) {
        this.subtract = subtract;

    }

    @ACAQDocumentation(name = "Background type", description = "Determines whether the background is dark or light.")
    @ACAQParameter("background-type")
    public BackgroundType getBackgroundType() {
        return backgroundType;
    }

    @ACAQParameter("background-type")
    public void setBackgroundType(BackgroundType backgroundType) {
        this.backgroundType = backgroundType;

    }

    @ACAQDocumentation(name = "Method", description = "The method to estimate the background")
    @ACAQParameter("method")
    public Method getMethod() {
        return method;
    }

    @ACAQParameter("method")
    public void setMethod(Method method) {
        this.method = method;
    }

    @ACAQDocumentation(name = "Apply pre-smoothing", description = "If enabled, a 3x3 box-filter is applied before extracting the background. " +
            "Please note that this might lead to the background not necessary being lower than the foreground.")
    @ACAQParameter("apply-pre-smoothing")
    public boolean isPreSmoothing() {
        return preSmoothing;
    }

    @ACAQParameter("apply-pre-smoothing")
    public void setPreSmoothing(boolean preSmoothing) {
        this.preSmoothing = preSmoothing;

    }

    @ACAQDocumentation(name = "Apply corner-correction", description = "If enabled, the algorithm attempts to avoid recognizing corners as background.")
    @ACAQParameter("apply-corner-correction")
    public boolean isCorrectCorners() {
        return correctCorners;
    }

    @ACAQParameter("apply-corner-correction")
    public void setCorrectCorners(boolean correctCorners) {
        this.correctCorners = correctCorners;

    }

    @ACAQDocumentation(name = "Separate channels", description = "Only valid for multi-channel images. If enabled, the algorithm is applied to each channel individually. " +
            "If disabled, the background is calculated based on the HSV brightness value. The hue is unaffected.")
    @ACAQParameter("separate-channels")
    public boolean isSeparateChannels() {
        return separateChannels;
    }

    @ACAQParameter("separate-channels")
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
