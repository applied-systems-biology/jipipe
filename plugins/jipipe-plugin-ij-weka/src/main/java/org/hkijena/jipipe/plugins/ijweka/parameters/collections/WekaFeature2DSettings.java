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

package org.hkijena.jipipe.plugins.ijweka.parameters.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijweka.parameters.features.WekaFeatureSet2D;

/**
 * Settings related to features
 */
public class WekaFeature2DSettings extends AbstractJIPipeParameterCollection {
    private WekaFeatureSet2D trainingFeatures = new WekaFeatureSet2D();
    /**
     * use neighborhood flag
     */
    private boolean useNeighbors = false;
    /**
     * expected membrane thickness (in pixels)
     */
    private int membraneSize = 1;
    /**
     * size of the patch to use to enhance membranes (in pixels, NxN)
     */
    private int membranePatchSize = 19;
    /**
     * number of rotating angles for membrane, Kuwahara and Gabor features
     */
    private float minSigma = 1;
    private float maxSigma = 16;

    public WekaFeature2DSettings() {

    }

    public WekaFeature2DSettings(WekaFeature2DSettings other) {
        this.trainingFeatures = new WekaFeatureSet2D(other.trainingFeatures);
        this.useNeighbors = other.useNeighbors;
        this.membraneSize = other.membraneSize;
        this.membranePatchSize = other.membranePatchSize;
        this.minSigma = other.minSigma;
        this.maxSigma = other.maxSigma;
    }

    @SetJIPipeDocumentation(name = "Use neighbors", description = "Set the use of the neighbor features")
    @JIPipeParameter("use-neighbors")
    public boolean isUseNeighbors() {
        return useNeighbors;
    }

    @JIPipeParameter("use-neighbors")
    public void setUseNeighbors(boolean useNeighbors) {
        this.useNeighbors = useNeighbors;
    }

    @SetJIPipeDocumentation(name = "Membrane thickness", description = "Expected value of the membrane thickness, 1 pixel by default." +
            " The more accurate, the more precise the filter will be. ")
    @JIPipeParameter(value = "membrane-size", uiOrder = -70)
    public int getMembraneSize() {
        return membraneSize;
    }

    @JIPipeParameter("membrane-size")
    public void setMembraneSize(int membraneSize) {
        this.membraneSize = membraneSize;
    }

    @SetJIPipeDocumentation(name = "Membrane patch size", description = "this represents the size \uD835\uDC5B×\uD835\uDC5B " +
            "of the field of view for the membrane projection filters. Only available for 2D features.")
    @JIPipeParameter(value = "membrane-patch-size", uiOrder = -60)
    public int getMembranePatchSize() {
        return membranePatchSize;
    }

    @JIPipeParameter("membrane-patch-size")
    public void setMembranePatchSize(int membranePatchSize) {
        this.membranePatchSize = membranePatchSize;
    }

    @SetJIPipeDocumentation(name = "Minimum sigma", description = "Minimum radius of the isotropic filters used to create the features. By default 1 pixel.")
    @JIPipeParameter(value = "min-sigma", uiOrder = -90)
    public float getMinSigma() {
        return minSigma;
    }

    @JIPipeParameter("min-sigma")
    public void setMinSigma(float minSigma) {
        this.minSigma = minSigma;
    }

    @SetJIPipeDocumentation(name = "Maximum sigma", description = "Maximum radius of the isotropic filters used to create the features. By default 16 pixels.")
    @JIPipeParameter(value = "max-sigma", uiOrder = -80)
    public float getMaxSigma() {
        return maxSigma;
    }

    @JIPipeParameter("max-sigma")
    public void setMaxSigma(float maxSigma) {
        this.maxSigma = maxSigma;
    }

    @SetJIPipeDocumentation(name = "Training features", description = "Here we can select and deselect the training features, which are the key of the learning procedure. " +
            "The plugin creates a stack of images —one image for each feature. " +
            "For instance, if only Gaussian blur is selected as a feature, the classifier will be trained on the original image and some blurred versions " +
            "to it with different \uD835\uDF0E parameters for the Gaussian. If the input image is grayscale, the features will be calculated using double precision (32-bit images). " +
            "In the case of RGB input images, the features will be RGB as well.")
    @JIPipeParameter(value = "training-features", uiOrder = -100)
    public WekaFeatureSet2D getTrainingFeatures() {
        return trainingFeatures;
    }

    @JIPipeParameter("training-features")
    public void setTrainingFeatures(WekaFeatureSet2D trainingFeatures) {
        this.trainingFeatures = trainingFeatures;
    }
}
