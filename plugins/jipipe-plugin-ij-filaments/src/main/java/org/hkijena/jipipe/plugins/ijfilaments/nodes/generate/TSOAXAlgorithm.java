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

import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Find and track filaments (TSOAX)", description = " TSOAX is an open source software to extract and track the growth and deformation of biopolymer networks from 2D and 3D time-lapse sequences. " +
        "It tracks each filament or network branch from complex network dynamics and works well even if filaments disappear or reappear. The output is a set of tracks for each evolving filament or network segment.\n" +
        "\n" +
        "TSOAX is an extension of SOAX (for network extraction in static images) to network extraction and tracking in time lapse movies.\n" +
        "\n" +
        "TSOAX facilitates quantitative analysis of network dynamics of multi-dimensional biopolymer networks imaged by various microscopic imaging modalities. The underlying methods of TSOAX includes multiple Stretching Open Active Contour Models for extraction and a combined local and global graph matching framework to establish temporal correspondence among all extracted structures.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@AddJIPipeCitation("T. Xu, C. Langouras, M. Adeli Koudehi, B. Vos, N. Wang, G. H. Koenderink, X. Huang and D. Vavylonis, \"Automated Tracking of Biopolymer Growth and Network Deformation with TSOAX\": Scientific Reports 9:1717 (2019)")
@AddJIPipeCitation("Website https://www.lehigh.edu/~div206/tsoax/index.html")
@AddJIPipeCitation("Documentation (SOAX) https://www.lehigh.edu/~div206/soax/doc/soax_manual.pdf")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Image", description = "The image to be analyzed", create = true)
public class TSOAXAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final ConvergenceParameters convergenceParameters;
    private final EvolutionParameters evolutionParameters;
    private final InitializationParameters initializationParameters;


    public TSOAXAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.convergenceParameters = new ConvergenceParameters();
        this.evolutionParameters = new EvolutionParameters();
        this.initializationParameters = new InitializationParameters();
        registerSubParameters(convergenceParameters, evolutionParameters, initializationParameters);
    }

    public TSOAXAlgorithm(TSOAXAlgorithm other) {
        super(other);
        this.convergenceParameters = new ConvergenceParameters(other.convergenceParameters);
        this.evolutionParameters = new EvolutionParameters(other.evolutionParameters);
        this.initializationParameters = new InitializationParameters(other.initializationParameters);
        registerSubParameters(convergenceParameters, evolutionParameters, initializationParameters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

    }

    @SetJIPipeDocumentation(name = "Convergence", description = "Controls how junctions are resolved to minimize sharp bends.")
    @JIPipeParameter(value = "convergence", uiOrder = -30, collapsed = true)
    public ConvergenceParameters getConvergenceParameters() {
        return convergenceParameters;
    }

    @SetJIPipeDocumentation(name = "Evolution", description = "Controls how the initial stretching active contours are stretched according to the local intensity contrast. Use the stretch factor to determine the overall elongation.")
    @JIPipeParameter(value = "evolution", uiOrder = -40, collapsed = true)
    public EvolutionParameters getEvolutionParameters() {
        return evolutionParameters;
    }

    @SetJIPipeDocumentation(name = "Initialization", description = "Determines at which intensity ridges the the initial stretching active contours are placed. Use the ridge threshold to determine the minimum intensity steepness.")
    @JIPipeParameter(value = "initialization", uiOrder = -50)
    public InitializationParameters getInitializationParameters() {
        return initializationParameters;
    }

    public static class InitializationParameters extends AbstractJIPipeParameterCollection {
        private double intensityScaling = 0;
        private double gaussianStd = 0;
        private double ridgeThreshold = 0.01;
        private double minimumForeground = 0;
        private double maximumForeground = 65535;
        private int snakePointSpacing = 1;
        private boolean initZ = true;

        public InitializationParameters() {
        }

        public InitializationParameters(InitializationParameters other) {
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
        public int getSnakePointSpacing() {
            return snakePointSpacing;
        }

        @JIPipeParameter("snake-point-spacing")
        public void setSnakePointSpacing(int snakePointSpacing) {
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

    public static class ConvergenceParameters extends AbstractJIPipeParameterCollection {
        private int minimumSnakeLength = 10;
        private int  maximumIterations = 10000;
        private int checkPeriod = 100;
        private double changeThreshold = 0.1;

        public ConvergenceParameters() {
        }

        public ConvergenceParameters(ConvergenceParameters other) {
            this.minimumSnakeLength = other.minimumSnakeLength;
            this.maximumIterations = other.maximumIterations;
            this.checkPeriod = other.checkPeriod;
            this.changeThreshold = other.changeThreshold;
        }

        @SetJIPipeDocumentation(name = "Minimum snake length", description = "(in pixels) specifies the minimum length of a resultant " +
                "snake. Increase the value to eliminate hair-like snake structures as well as to avoid the snakes picking " +
                "up actin patches in yeast images")
        @JIPipeParameter(value = "minimum-snake-length", important = true)
        public int getMinimumSnakeLength() {
            return minimumSnakeLength;
        }

        @JIPipeParameter("minimum-snake-length")
        public void setMinimumSnakeLength(int minimumSnakeLength) {
            this.minimumSnakeLength = minimumSnakeLength;
        }

        @SetJIPipeDocumentation(name = "Maximum iterations", description = "Specifies the maximum number of iterations allowed in each snake evolution")
        @JIPipeParameter("maximum-iterations")
        public int getMaximumIterations() {
            return maximumIterations;
        }

        @JIPipeParameter("maximum-iterations")
        public void setMaximumIterations(int maximumIterations) {
            this.maximumIterations = maximumIterations;
        }

        @SetJIPipeDocumentation(name = "Check period", description = "Specifies the cycle of checking for convergence in number of iterations. A value of 100 means " +
                "a snake is checked for convergence every 100 iterations. (see the last sentence of Section 2.1 in https://doi.org/10.1016/j.media.2013.10.015)")
        @JIPipeParameter("check-period")
        public int getCheckPeriod() {
            return checkPeriod;
        }

        @JIPipeParameter("check-period")
        public void setCheckPeriod(int checkPeriod) {
            this.checkPeriod = checkPeriod;
        }

        @SetJIPipeDocumentation(name = "Change threshold", description = "(in pixels) specifies the threshold of change for a snake to be converged. A value of " +
                "0.05 means a snake is converged if every snake point drifts less than 0.05 pixels since last check for " +
                "convergence (see the last sentence of Section 2.1 in https://doi.org/10.1016/j.media.2013.10.015)")
        @JIPipeParameter("change-threshold")
        public double getChangeThreshold() {
            return changeThreshold;
        }

        @JIPipeParameter("change-threshold")
        public void setChangeThreshold(double changeThreshold) {
            this.changeThreshold = changeThreshold;
        }
    }

    public static class EvolutionParameters extends AbstractJIPipeParameterCollection {
        private double alpha = 0.01;
        private double beta = 0.1;
        private double gamma = 2;
        private double externalFactor = 1;
        private double stretchFactor = 0.2;
        private int numberOfBackgroundRadialSectors = 8;
        private double radialNear = 4;
        private double radialFar = 8;
        private double backgroundZXYRatio = 1;
        private double delta = 4;
        private double overlapThreshold = 1;
        private double groupingDistanceThreshold = 4;
        private int groupingDelta = 8;
        private double minimumAngleForSOACLinking = 2.1;
        private boolean dampZ = false;
        private double c = 0.2;
        private boolean grouping = true;

        public EvolutionParameters() {

        }

        public EvolutionParameters(EvolutionParameters other) {
            this.alpha = other.alpha;
            this.beta = other.beta;
            this.gamma = other.gamma;
            this.externalFactor = other.externalFactor;
            this.stretchFactor = other.stretchFactor;
            this.numberOfBackgroundRadialSectors = other.numberOfBackgroundRadialSectors;
            this.radialNear = other.radialNear;
            this.radialFar = other.radialFar;
            this.backgroundZXYRatio = other.backgroundZXYRatio;
            this.delta = other.delta;
            this.overlapThreshold = other.overlapThreshold;
            this.groupingDistanceThreshold = other.groupingDistanceThreshold;
            this.groupingDelta = other.groupingDelta;
            this.minimumAngleForSOACLinking = other.minimumAngleForSOACLinking;
            this.dampZ = other.dampZ;
            this.c = other.c;
            this.grouping = other.grouping;
        }

        @SetJIPipeDocumentation(name = "Alpha (elongation penalty)", description = "The weight of first order continuity of snake (see Equation 1 in https://doi.org/10.1016/j.media.2013.10.015). This term describes the " +
                "energy penalty to elongate snakes. For images with dim linear structures and bright spots, one may " +
                "want to use small value of alpha")
        @JIPipeParameter("alpha")
        public double getAlpha() {
            return alpha;
        }

        @JIPipeParameter("alpha")
        public void setAlpha(double alpha) {
            this.alpha = alpha;
        }

        @SetJIPipeDocumentation(name = "Beta (bending penalty)", description = "The weight of second order continuity of snake (see Eq.1 in https://doi.org/10.1016/j.media.2013.10.015). This term describes the snake " +
                "bending energy penalty. Use larger value to make snakes more straight")
        @JIPipeParameter("beta")
        public double getBeta() {
            return beta;
        }

        @JIPipeParameter("beta")
        public void setBeta(double beta) {
            this.beta = beta;
        }

        @SetJIPipeDocumentation(name = "Gamma (evolution step size)", description = "Controls the step size of the iterative process of snake evolution. The smaller gamma is, the " +
                "faster snakes converge but the result is less accurate (see Equation 6 in https://doi.org/10.1016/j.media.2013.10.015)")
        @JIPipeParameter("gamma")
        public double getGamma() {
            return gamma;
        }

        @JIPipeParameter("gamma")
        public void setGamma(double gamma) {
            this.gamma = gamma;
        }

        @SetJIPipeDocumentation(name = "External factor", description = "The weight of image forces (image gradient) (see Equation 2 in " +
                "https://doi.org/10.1016/j.media.2013.10.015). Increasing this value to make snakes follow more closely the local shape of filaments")
        @JIPipeParameter("external-factor")
        public double getExternalFactor() {
            return externalFactor;
        }

        @JIPipeParameter("external-factor")
        public void setExternalFactor(double externalFactor) {
            this.externalFactor = externalFactor;
        }

        @SetJIPipeDocumentation(name = "Stretch factor (bridging)", description = "The weight of stretching force (see Equation 2 in https://doi.org/10.1016/j.media.2013.10.015). Increasing " +
                "this value to stretch snakes more in case of under-segmentation.")
        @JIPipeParameter(value = "stretch-factor", important = true)
        public double getStretchFactor() {
            return stretchFactor;
        }

        @JIPipeParameter("stretch-factor")
        public void setStretchFactor(double stretchFactor) {
            this.stretchFactor = stretchFactor;
        }

        @SetJIPipeDocumentation(name = "Number of radial sectors", description = "Together with other radial parameters, define the local annulus from which magnitude of stretching forces (see Section 2.1.1 and Figure 3 in https://doi.org/10.1016/j.media.2013.10.015) and local image SNR are computed.")
        @JIPipeParameter("number-of-background-radial-sectors")
        public int getNumberOfBackgroundRadialSectors() {
            return numberOfBackgroundRadialSectors;
        }

        @JIPipeParameter("number-of-background-radial-sectors")
        public void setNumberOfBackgroundRadialSectors(int numberOfBackgroundRadialSectors) {
            this.numberOfBackgroundRadialSectors = numberOfBackgroundRadialSectors;
        }

        @SetJIPipeDocumentation(name = "Radial near", description = "Together with other radial parameters, define the local annulus from which magnitude of stretching forces (see Section 2.1.1 and Figure 3 in https://doi.org/10.1016/j.media.2013.10.015) and local image SNR are computed.")
        @JIPipeParameter("radial-near")
        public double getRadialNear() {
            return radialNear;
        }

        @JIPipeParameter("radial-near")
        public void setRadialNear(double radialNear) {
            this.radialNear = radialNear;
        }

        @SetJIPipeDocumentation(name = "Radial far", description = "Together with other radial parameters, define the local annulus from which magnitude of stretching forces (see Section 2.1.1 and Figure 3 in https://doi.org/10.1016/j.media.2013.10.015) and local image SNR are computed.")
        @JIPipeParameter("radial-far")
        public double getRadialFar() {
            return radialFar;
        }

        @JIPipeParameter("radial-far")
        public void setRadialFar(double radialFar) {
            this.radialFar = radialFar;
        }

        @SetJIPipeDocumentation(name = "Background Z/XY ratio", description = "Defines the anisotropy of the PSF of microscope. It is the spreading of PSF " +
                "along z-axis relative to that of x and y-axis. Set this parameter to fix the anisotropy in the background " +
                "intensity calculation")
        @JIPipeParameter("background-z-xy-ratio")
        public double getBackgroundZXYRatio() {
            return backgroundZXYRatio;
        }

        @JIPipeParameter("background-z-xy-ratio")
        public void setBackgroundZXYRatio(double backgroundZXYRatio) {
            this.backgroundZXYRatio = backgroundZXYRatio;
        }

        @SetJIPipeDocumentation(name = "Delta", description = "The delta (number of snake points apart) for computing snake tip tangent using finite difference. Must be a positive integer")
        @JIPipeParameter("delta")
        public double getDelta() {
            return delta;
        }

        @JIPipeParameter("delta")
        public void setDelta(double delta) {
            this.delta = delta;
        }

        @SetJIPipeDocumentation(name = "Overlap threshold", description = "The distance threshold that snakes are considered overlapping")
        @JIPipeParameter("overlap-threshold")
        public double getOverlapThreshold() {
            return overlapThreshold;
        }

        @JIPipeParameter("overlap-threshold")
        public void setOverlapThreshold(double overlapThreshold) {
            this.overlapThreshold = overlapThreshold;
        }

        @SetJIPipeDocumentation(name = "Grouping distance threshold", description = "The maximum distance that two T-junctions formed after snake " +
                "evolution can be clustered into one clustered junction for grouping. Large values may help collapsing " +
                "unwanted T-junctions.")
        @JIPipeParameter("grouping-distance-threshold")
        public double getGroupingDistanceThreshold() {
            return groupingDistanceThreshold;
        }

        @JIPipeParameter("grouping-distance-threshold")
        public void setGroupingDistanceThreshold(double groupingDistanceThreshold) {
            this.groupingDistanceThreshold = groupingDistanceThreshold;
        }

        @SetJIPipeDocumentation(name = "Grouping delta", description = "Specifies the delta (number of snake points apart) for computing tip tangents of dissected " +
                "snake segments using finite difference. Must be a positive integer")
        @JIPipeParameter("grouping-delta")
        public int getGroupingDelta() {
            return groupingDelta;
        }

        @JIPipeParameter("grouping-delta")
        public void setGroupingDelta(int groupingDelta) {
            this.groupingDelta = groupingDelta;
        }

        @SetJIPipeDocumentation(name = "Minimum angle for SOAC linking", description = "(in radians) is the angular threshold for grouping snakes. The " +
                "angle between the tangent directions of two snake branches in a clustered junction must be greater " +
                "than this value to be grouped. Default is 2π/3")
        @JIPipeParameter("minimum-angle-for-soac-linking")
        public double getMinimumAngleForSOACLinking() {
            return minimumAngleForSOACLinking;
        }

        @JIPipeParameter("minimum-angle-for-soac-linking")
        public void setMinimumAngleForSOACLinking(double minimumAngleForSOACLinking) {
            this.minimumAngleForSOACLinking = minimumAngleForSOACLinking;
        }

        @SetJIPipeDocumentation(name = "Damp Z", description = "Toggles the suppression of snake evolution along the z-axis. This may be useful when anisotropy in PSF along z becomes a problem")
        @JIPipeParameter("damp-z")
        public boolean isDampZ() {
            return dampZ;
        }

        @JIPipeParameter("damp-z")
        public void setDampZ(boolean dampZ) {
            this.dampZ = dampZ;
        }

        @SetJIPipeDocumentation(name = "c (track continuity)", description = "The weight by which snakes detected at nearby locations in space are assigned to the same track, as a function of the number of time frames separating them. The value of 1/c is the number of frames beyond which the probability of assigning snakes to the same track decreases exponentially with frame number separation. This parameter should be increased (up to order 1) to improve track continuity over successive frames.")
        @JIPipeParameter("c")
        public double getC() {
            return c;
        }

        @JIPipeParameter("c")
        public void setC(double c) {
            this.c = c;
        }

        @SetJIPipeDocumentation(name = "Grouping", description = "Enables the grouping process of snakes at detected junctions prior to tracking. This option can be enabled for tracking of intersecting elongating filaments and disabled for tracking the movement of filament segments in between junction points. ")
        @JIPipeParameter("grouping")
        public boolean isGrouping() {
            return grouping;
        }

        @JIPipeParameter("grouping")
        public void setGrouping(boolean grouping) {
            this.grouping = grouping;
        }
    }
}
