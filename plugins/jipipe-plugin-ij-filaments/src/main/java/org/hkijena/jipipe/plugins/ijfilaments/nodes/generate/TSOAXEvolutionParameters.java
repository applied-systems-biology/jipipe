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

public class TSOAXEvolutionParameters extends AbstractJIPipeParameterCollection {
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
    private double associationThreshold = 10;

    public TSOAXEvolutionParameters() {

    }

    public TSOAXEvolutionParameters(TSOAXEvolutionParameters other) {
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
        this.associationThreshold = other.associationThreshold;
    }

    @SetJIPipeDocumentation(name = "Association threshold", description = "(in pixels). Not documented by original developers.")
    @JIPipeParameter("association-threshold")
    public double getAssociationThreshold() {
        return associationThreshold;
    }

    @JIPipeParameter("association-threshold")
    public void setAssociationThreshold(double associationThreshold) {
        this.associationThreshold = associationThreshold;
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
