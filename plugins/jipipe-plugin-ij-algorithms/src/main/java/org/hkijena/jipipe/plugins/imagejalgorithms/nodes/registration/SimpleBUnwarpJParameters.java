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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

/**
 * Reduced parameter set for {@link SimpleImageRegistrationAlgorithm} (no shear, scale, anisotropy correction)
 */
public class SimpleBUnwarpJParameters extends AbstractJIPipeParameterCollection {
    private BUnwarpJMode mode = BUnwarpJMode.Mono;
    private int imageSubsamplingFactor = 0;
    private BUnwarpJMinScaleDeformation minScaleDeformation = BUnwarpJMinScaleDeformation.VeryCoarse;
    private BUnwarpJMaxScaleDeformation maxScaleDeformation = BUnwarpJMaxScaleDeformation.Fine;
    private double divergenceWeight = 0;
    private double curlWeight = 0;
    private double landmarkWeight = 0;
    private double imageWeight = 1;
    private double consistencyWeight = 10;
    private double stopThreshold = 0.01;

    public SimpleBUnwarpJParameters() {
    }

    public SimpleBUnwarpJParameters(SimpleBUnwarpJParameters other) {
        this.mode = other.mode;
        this.imageSubsamplingFactor = other.imageSubsamplingFactor;
        this.minScaleDeformation = other.minScaleDeformation;
        this.maxScaleDeformation = other.maxScaleDeformation;
        this.divergenceWeight = other.divergenceWeight;
        this.curlWeight = other.curlWeight;
        this.landmarkWeight = other.landmarkWeight;
        this.imageWeight = other.imageWeight;
        this.consistencyWeight = other.consistencyWeight;
        this.stopThreshold = other.stopThreshold;
    }

    @SetJIPipeDocumentation(name = "Registration mode", description = "“Mono”, “Accurate” or “Fast”. " +
            "“Mono” means unidirectional registration and its actually the fastest of the three modes. “Accurate” and “Fast” perform bidirectional registration and take more time.")
    @JIPipeParameter(value = "mode", important = true)
    public BUnwarpJMode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(BUnwarpJMode mode) {
        this.mode = mode;
    }

    @SetJIPipeDocumentation(name = "Image subsampling factor", description = "Ranges from 0 to 7, representing 2^0=1 to 2^7 = 128. We recommend to use it if the images are very large.")
    @JIPipeParameter("image-subsampling-factor")
    public int getImageSubsamplingFactor() {
        return imageSubsamplingFactor;
    }

    @JIPipeParameter("image-subsampling-factor")
    public void setImageSubsamplingFactor(int imageSubsamplingFactor) {
        this.imageSubsamplingFactor = imageSubsamplingFactor;
    }

    @SetJIPipeDocumentation(name = "Minimum scale deformation", description = "Defines the number of B-spline coefficients in the deformation grid (from 2x2 to 16x16). " +
            "More coefficients mean more precision but also the possibility of over-registering.")
    @JIPipeParameter("min-scale-deformation")
    public BUnwarpJMinScaleDeformation getMinScaleDeformation() {
        return minScaleDeformation;
    }

    @JIPipeParameter("min-scale-deformation")
    public void setMinScaleDeformation(BUnwarpJMinScaleDeformation minScaleDeformation) {
        this.minScaleDeformation = minScaleDeformation;
    }

    @SetJIPipeDocumentation(name = "Maximum scale deformation", description = "Defines the number of B-spline coefficients in the deformation grid (from 2x2 to 16x16). " +
            "More coefficients mean more precision but also the possibility of over-registering.")
    @JIPipeParameter("max-scale-deformation")
    public BUnwarpJMaxScaleDeformation getMaxScaleDeformation() {
        return maxScaleDeformation;
    }

    @JIPipeParameter("max-scale-deformation")
    public void setMaxScaleDeformation(BUnwarpJMaxScaleDeformation maxScaleDeformation) {
        this.maxScaleDeformation = maxScaleDeformation;
    }

    @SetJIPipeDocumentation(name = "Divergence weight", description = "Regularizes the deformation to make it smooth")
    @JIPipeParameter("divergence-weight")
    public double getDivergenceWeight() {
        return divergenceWeight;
    }

    @JIPipeParameter("divergence-weight")
    public void setDivergenceWeight(double divergenceWeight) {
        this.divergenceWeight = divergenceWeight;
    }

    @SetJIPipeDocumentation(name = "Curl weight", description = "Regularizes the deformation to make it smooth")
    @JIPipeParameter("curl-weight")
    public double getCurlWeight() {
        return curlWeight;
    }

    @JIPipeParameter("curl-weight")
    public void setCurlWeight(double curlWeight) {
        this.curlWeight = curlWeight;
    }

    @SetJIPipeDocumentation(name = "Landmark weight", description = "Relevance of adjusting the correspondences found in the feature extraction.")
    @JIPipeParameter("landmark-weight")
    public double getLandmarkWeight() {
        return landmarkWeight;
    }

    @JIPipeParameter("landmark-weight")
    public void setLandmarkWeight(double landmarkWeight) {
        this.landmarkWeight = landmarkWeight;
    }

    @SetJIPipeDocumentation(name = "Image weight", description = "Relevance of the similarity between source and target image in the energy function.")
    @JIPipeParameter("image-weight")
    public double getImageWeight() {
        return imageWeight;
    }

    @JIPipeParameter("image-weight")
    public void setImageWeight(double imageWeight) {
        this.imageWeight = imageWeight;
    }

    @SetJIPipeDocumentation(name = "Consistency weight", description = "Relevance of the consistency error between the direct and inverse deformations (only for “Accurate” or “Fast” modes).")
    @JIPipeParameter("consistency-weight")
    public double getConsistencyWeight() {
        return consistencyWeight;
    }

    @JIPipeParameter("consistency-weight")
    public void setConsistencyWeight(double consistencyWeight) {
        this.consistencyWeight = consistencyWeight;
    }

    @SetJIPipeDocumentation(name = "Stop threshold", description = "Difference between the last and previous algorithm iterations that makes the registration to end.")
    @JIPipeParameter("stop-threshold")
    public double getStopThreshold() {
        return stopThreshold;
    }

    @JIPipeParameter("stop-threshold")
    public void setStopThreshold(double stopThreshold) {
        this.stopThreshold = stopThreshold;
    }
}
