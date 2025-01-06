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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.simple;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class SimpleImageRegistrationParameters extends AbstractJIPipeParameterCollection {
    private SimpleImageRegistrationModel imageRegistrationModel = SimpleImageRegistrationModel.Rigid;
    private SimpleImageRegistrationFeatureModel imageRegistrationFeatureModel = SimpleImageRegistrationFeatureModel.Rigid;
    private float rod = 0.92f;
    private float maxEpsilon = 25.0f;
    private float minInlierRatio = 0.05f;
    private boolean interpolate = true;

    public SimpleImageRegistrationParameters() {
    }

    public SimpleImageRegistrationParameters(SimpleImageRegistrationParameters other) {
        this.imageRegistrationModel = other.imageRegistrationModel;
        this.imageRegistrationFeatureModel = other.imageRegistrationFeatureModel;
        this.rod = other.rod;
        this.maxEpsilon = other.maxEpsilon;
        this.minInlierRatio = other.minInlierRatio;
        this.interpolate = other.interpolate;
    }

    @SetJIPipeDocumentation(name = "Image registration model", description = "The image registration method." +
            "<ul>" +
            "<li>Translation: no deformation</li>" +
            "<li>Rigid: translate + rotate</li>" +
            "<li>Similarity: translate + rotate + isotropic scale</li>" +
            "<li>Affine: free affine transform</li>" +
            "<li>Elastic: BUnwarpJ splines</li>" +
            "<li>Moving least squares: maximal warping</li>" +
            "</ul>")
    @JIPipeParameter(value = "image-registration-model", important = true)
    public SimpleImageRegistrationModel getImageRegistrationModel() {
        return imageRegistrationModel;
    }

    @JIPipeParameter("image-registration-model")
    public void setImageRegistrationModel(SimpleImageRegistrationModel imageRegistrationModel) {
        this.imageRegistrationModel = imageRegistrationModel;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Image feature extraction model", description = " The expected transformation model finding inliers" +
            " (i.e. correspondences or landmarks between images) in the feature extraction.")
    @JIPipeParameter(value = "image-registration-feature-model", important = true)
    public SimpleImageRegistrationFeatureModel getImageRegistrationFeatureModel() {
        return imageRegistrationFeatureModel;
    }

    @JIPipeParameter("image-registration-feature-model")
    public void setImageRegistrationFeatureModel(SimpleImageRegistrationFeatureModel imageRegistrationFeatureModel) {
        this.imageRegistrationFeatureModel = imageRegistrationFeatureModel;
    }

    @SetJIPipeDocumentation(name = "Ratio of distances", description = "Closest/ next neighbor distance ratio")
    @JIPipeParameter("rod")
    public float getRod() {
        return rod;
    }

    @JIPipeParameter("rod")
    public void setRod(float rod) {
        this.rod = rod;
    }

    @SetJIPipeDocumentation(name = "Max epsilon", description = "Maximal allowed alignment error in pixels")
    @JIPipeParameter("max-epsilon")
    public float getMaxEpsilon() {
        return maxEpsilon;
    }

    @JIPipeParameter("max-epsilon")
    public void setMaxEpsilon(float maxEpsilon) {
        this.maxEpsilon = maxEpsilon;
    }

    @SetJIPipeDocumentation(name = "Inlier/candidates ratio")
    @JIPipeParameter("min-inlier-ratio")
    public float getMinInlierRatio() {
        return minInlierRatio;
    }

    @JIPipeParameter("min-inlier-ratio")
    public void setMinInlierRatio(float minInlierRatio) {
        this.minInlierRatio = minInlierRatio;
    }

    @SetJIPipeDocumentation(name = "Interpolate")
    @JIPipeParameter("interpolate")
    public boolean isInterpolate() {
        return interpolate;
    }

    @JIPipeParameter("interpolate")
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }
}
