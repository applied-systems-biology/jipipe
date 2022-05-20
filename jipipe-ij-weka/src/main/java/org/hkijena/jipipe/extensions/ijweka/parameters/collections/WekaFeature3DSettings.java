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
 *
 */

package org.hkijena.jipipe.extensions.ijweka.parameters.collections;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.ijweka.parameters.features.WekaFeatureSet3D;

/**
 * Settings related to features
 */
public class WekaFeature3DSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private WekaFeatureSet3D trainingFeatures = new WekaFeatureSet3D();
    /** use neighborhood flag */
    private boolean useNeighbors = false;
    /** expected membrane thickness (in pixels) */
    private int membraneSize = 1;
    /** size of the patch to use to enhance membranes (in pixels, NxN) */
    private int membranePatchSize = 19;
    /** number of rotating angles for membrane, Kuwahara and Gabor features */
    private float minSigma = 1;
    private float maxSigma = 16;

    public WekaFeature3DSettings() {

    }

    public WekaFeature3DSettings(WekaFeature3DSettings other) {
        this.trainingFeatures = new WekaFeatureSet3D(other.trainingFeatures);
        this.useNeighbors = other.useNeighbors;
        this.membraneSize = other.membraneSize;
        this.membranePatchSize = other.membranePatchSize;
        this.minSigma = other.minSigma;
        this.maxSigma = other.maxSigma;
    }

    @JIPipeDocumentation(name = "Use neighbors", description = "Set the use of the neighbor features")
    @JIPipeParameter("use-neighbors")
    public boolean isUseNeighbors() {
        return useNeighbors;
    }

    @JIPipeParameter("use-neighbors")
    public void setUseNeighbors(boolean useNeighbors) {
        this.useNeighbors = useNeighbors;
    }

    @JIPipeDocumentation(name = "Membrane thickness", description = "Expected value of the membrane thickness, 1 pixel by default." +
            " The more accurate, the more precise the filter will be. ")
    @JIPipeParameter(value = "membrane-size", uiOrder = -70)
    public int getMembraneSize() {
        return membraneSize;
    }

    @JIPipeParameter("membrane-size")
    public void setMembraneSize(int membraneSize) {
        this.membraneSize = membraneSize;
    }

    @JIPipeDocumentation(name = "Membrane patch size", description = "this represents the size \uD835\uDC5B×\uD835\uDC5B " +
            "of the field of view for the membrane projection filters. Only available for 2D features.")
    @JIPipeParameter(value = "membrane-patch-size", uiOrder = -60)
    public int getMembranePatchSize() {
        return membranePatchSize;
    }

    @JIPipeParameter("membrane-patch-size")
    public void setMembranePatchSize(int membranePatchSize) {
        this.membranePatchSize = membranePatchSize;
    }

    @JIPipeDocumentation(name = "Minimum sigma", description = "Minimum radius of the isotropic filters used to create the features. By default 1 pixel.")
    @JIPipeParameter(value = "min-sigma", uiOrder = -90)
    public float getMinSigma() {
        return minSigma;
    }

    @JIPipeParameter("min-sigma")
    public void setMinSigma(float minSigma) {
        this.minSigma = minSigma;
    }

    @JIPipeDocumentation(name = "Maximum sigma", description = "Maximum radius of the isotropic filters used to create the features. By default 16 pixels.")
    @JIPipeParameter(value = "max-sigma", uiOrder = -80)
    public float getMaxSigma() {
        return maxSigma;
    }

    @JIPipeParameter("max-sigma")
    public void setMaxSigma(float maxSigma) {
        this.maxSigma = maxSigma;
    }

    @JIPipeDocumentation(name = "Training features", description = "Here we can select and deselect the training features, which are the key of the learning procedure. " +
            "The plugin creates a stack of images —one image for each feature. " +
            "For instance, if only Gaussian blur is selected as a feature, the classifier will be trained on the original image and some blurred versions " +
            "to it with different \uD835\uDF0E parameters for the Gaussian. If the input image is grayscale, the features will be calculated using double precision (32-bit images). " +
            "In the case of RGB input images, the features will be RGB as well.")
    @JIPipeParameter(value = "training-features", uiOrder = -100)
    public WekaFeatureSet3D getTrainingFeatures() {
        return trainingFeatures;
    }

    @JIPipeParameter("training-features")
    public void setTrainingFeatures(WekaFeatureSet3D trainingFeatures) {
        this.trainingFeatures = trainingFeatures;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
