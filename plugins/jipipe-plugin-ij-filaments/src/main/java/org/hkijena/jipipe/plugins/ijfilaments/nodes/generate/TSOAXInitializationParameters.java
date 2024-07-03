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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.generate;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class TSOAXInitializationParameters extends AbstractJIPipeParameterCollection {
    private double intensityScaling = 0;
    private double gaussianStd = 0;
    private double ridgeThreshold = 0.01;
    private double minimumForeground = 0;
    private double maximumForeground = 65535;
    private double snakePointSpacing = 1;
    private boolean initZ = true;

    public TSOAXInitializationParameters() {
    }

    public TSOAXInitializationParameters(TSOAXInitializationParameters other) {
        this.intensityScaling = other.intensityScaling;
        this.gaussianStd = other.gaussianStd;
        this.ridgeThreshold = other.ridgeThreshold;
        this.minimumForeground = other.minimumForeground;
        this.maximumForeground = other.maximumForeground;
        this.snakePointSpacing = other.snakePointSpacing;
        this.initZ = other.initZ;
    }

    @SetJIPipeDocumentation(name = "Intensity scaling", description = "Multiplies the intensity of every pixel such that the range of rescaled intensities lie " +
            "roughly between 0.0 and 1.0. This allows using a standard range of α, β and other parameters below. " +
            "Leave fixed for a given set of images. Set this value to 0 for automatic scaling, where the maximum " +
            "intensity is scaled to exact 1.0.")
    @JIPipeParameter("intensity-scaling")
    public double getIntensityScaling() {
        return intensityScaling;
    }

    @JIPipeParameter("intensity-scaling")
    public void setIntensityScaling(double intensityScaling) {
        this.intensityScaling = intensityScaling;
    }

    @SetJIPipeDocumentation(name = "Gaussian std", description = "(in pixels) controls the amount of Gaussian smoothing in the computation of image gradient (See Section 2.1 and Equation 3 in Xu, T., Vavylonis, D. & Huang, X. 3D actin network centerline extraction with multiple active contours. " +
            "Medical Image Analysis 18, 272–284 (2014).). Set σ < 0.01 to disable smoothing")
    @JIPipeParameter("gaussian-std")
    public double getGaussianStd() {
        return gaussianStd;
    }

    @JIPipeParameter("gaussian-std")
    public void setGaussianStd(double gaussianStd) {
        this.gaussianStd = gaussianStd;
    }

    @SetJIPipeDocumentation(name = "Ridge threshold", description = "(also “grad-diff”) controls the number of initialized snakes (see Section 2.2.1 in https://doi.org/10.1016/j.media.2013.10.015). Decrease this value to initialize more snakes")
    @JIPipeParameter(value = "ridge-threshold", important = true)
    public double getRidgeThreshold() {
        return ridgeThreshold;
    }

    @JIPipeParameter("ridge-threshold")
    public void setRidgeThreshold(double ridgeThreshold) {
        this.ridgeThreshold = ridgeThreshold;
    }

    @SetJIPipeDocumentation(name = "Minimum foreground", description = "Specifies the range of intensities intended for extraction. Snakes " +
            "are not initialized where the image intensity is below “background” or above “foreground”. During " +
            "evolution, stretching force is zero when the intensity at a snake tip is outside this range.")
    @JIPipeParameter("minimum-foreground")
    public double getMinimumForeground() {
        return minimumForeground;
    }

    @JIPipeParameter("minimum-foreground")
    public void setMinimumForeground(double minimumForeground) {
        this.minimumForeground = minimumForeground;
    }

    @SetJIPipeDocumentation(name = "Maximum foreground", description = "Specifies the range of intensities intended for extraction. Snakes " +
            "are not initialized where the image intensity is below “background” or above “foreground”. During " +
            "evolution, stretching force is zero when the intensity at a snake tip is outside this range.")
    @JIPipeParameter("maximum-foreground")
    public double getMaximumForeground() {
        return maximumForeground;
    }

    @JIPipeParameter("maximum-foreground")
    public void setMaximumForeground(double maximumForeground) {
        this.maximumForeground = maximumForeground;
    }

    @SetJIPipeDocumentation(name = "Snake point spacing", description = "(in pixels) specifies the spacing between consecutive snake points (see the end of Section 2.1 in https://doi.org/10.1016/j.media.2013.10.015).")
    @JIPipeParameter(value = "snake-point-spacing", important = true)
    public double getSnakePointSpacing() {
        return snakePointSpacing;
    }

    @JIPipeParameter("snake-point-spacing")
    public void setSnakePointSpacing(double snakePointSpacing) {
        this.snakePointSpacing = snakePointSpacing;
    }

    @SetJIPipeDocumentation(name = "Init z", description = "Toggles the initialization of snakes along z-axis. Uncheck it to eliminate snakes that are perpendicular " +
            "to filaments due to anisotropic PSF with larger spreading along z-axis.")
    @JIPipeParameter("init-z")
    public boolean isInitZ() {
        return initZ;
    }

    @JIPipeParameter("init-z")
    public void setInitZ(boolean initZ) {
        this.initZ = initZ;
    }
}
