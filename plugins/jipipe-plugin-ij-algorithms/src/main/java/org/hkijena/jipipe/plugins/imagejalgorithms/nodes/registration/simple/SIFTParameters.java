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

import mpicbg.imagefeatures.FloatArray2DSIFT;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;

/**
 * Wrapper around {@link mpicbg.imagefeatures.FloatArray2DSIFT} params
 */
public class SIFTParameters extends AbstractJIPipeParameterCollection {
    private int featureDescriptorSize = 4;
    private int featureDescriptorOrientationBins = 8;
    private Vector2iParameter octaveSizeLimits = new Vector2iParameter(64, 1024);
    private int stepsPerScale = 3;
    private float initialSigma = 1.6f;

    public SIFTParameters() {
    }

    public SIFTParameters(SIFTParameters other) {
        this.featureDescriptorSize = other.featureDescriptorSize;
        this.featureDescriptorOrientationBins = other.featureDescriptorOrientationBins;
        this.octaveSizeLimits = new Vector2iParameter(other.octaveSizeLimits);
        this.stepsPerScale = other.stepsPerScale;
        this.initialSigma = other.initialSigma;
    }

    @SetJIPipeDocumentation(name = "Feature descriptor size", description = "How many samples per row and column")
    @JIPipeParameter("feature-descriptor-size")
    public int getFeatureDescriptorSize() {
        return featureDescriptorSize;
    }

    @JIPipeParameter("feature-descriptor-size")
    public void setFeatureDescriptorSize(int featureDescriptorSize) {
        this.featureDescriptorSize = featureDescriptorSize;
    }

    @SetJIPipeDocumentation(name = "Feature descriptor orientation bins", description = "How many bins per local histogram")
    @JIPipeParameter("feature-descriptor-orientation-bins")
    public int getFeatureDescriptorOrientationBins() {
        return featureDescriptorOrientationBins;
    }

    @JIPipeParameter("feature-descriptor-orientation-bins")
    public void setFeatureDescriptorOrientationBins(int featureDescriptorOrientationBins) {
        this.featureDescriptorOrientationBins = featureDescriptorOrientationBins;
    }

    @SetJIPipeDocumentation(name = "Scale octaves size limits", description = "Size limits for scale octaves in px: minOctaveSize < octave < maxOctaveSize")
    @JIPipeParameter("octave-size-limits")
    @VectorParameterSettings(xLabel = "Min", yLabel = "Max")
    public Vector2iParameter getOctaveSizeLimits() {
        return octaveSizeLimits;
    }

    @JIPipeParameter("octave-size-limits")
    public void setOctaveSizeLimits(Vector2iParameter octaveSizeLimits) {
        this.octaveSizeLimits = octaveSizeLimits;
    }

    @SetJIPipeDocumentation(name = "Steps per scale octave")
    @JIPipeParameter("steps-per-scale")
    public int getStepsPerScale() {
        return stepsPerScale;
    }

    @JIPipeParameter("steps-per-scale")
    public void setStepsPerScale(int stepsPerScale) {
        this.stepsPerScale = stepsPerScale;
    }

    @SetJIPipeDocumentation(name = "Initial Gaussian blur sigma", description = "Initial sigma of each Scale Octave")
    @JIPipeParameter("initial-sigma")
    public float getInitialSigma() {
        return initialSigma;
    }

    @JIPipeParameter("initial-sigma")
    public void setInitialSigma(float initialSigma) {
        this.initialSigma = initialSigma;
    }

    public FloatArray2DSIFT.Param toParam() {
        FloatArray2DSIFT.Param param = new FloatArray2DSIFT.Param();
        param.initialSigma = initialSigma;
        param.fdBins = featureDescriptorOrientationBins;
        param.fdSize = featureDescriptorSize;
        param.minOctaveSize = octaveSizeLimits.getX();
        param.maxOctaveSize = octaveSizeLimits.getY();
        param.steps = stepsPerScale;
        return param;
    }
}
