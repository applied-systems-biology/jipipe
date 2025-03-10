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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp3;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

public class Cellpose3DenoiseTrainingNoiseSettings extends AbstractJIPipeParameterCollection {
    private Cellpose3DenoiseTrainingNoiseType noiseType = Cellpose3DenoiseTrainingNoiseType.poisson;
    private double customPoisson = 0.8;
    private double customBeta = 0.7;
    private double customBlur = 0;
    private double customGBlur = 1;
    private double customDownsample = 0;
    private OptionalIntegerParameter maxDownsampling = new OptionalIntegerParameter(false, 7);

    public Cellpose3DenoiseTrainingNoiseSettings() {
    }

    public Cellpose3DenoiseTrainingNoiseSettings(Cellpose3DenoiseTrainingNoiseSettings other) {
        this.noiseType = other.noiseType;
        this.customPoisson = other.customPoisson;
        this.customBeta = other.customBeta;
        this.customBlur = other.customBlur;
        this.customGBlur = other.customGBlur;
        this.customDownsample = other.customDownsample;
        this.maxDownsampling = new OptionalIntegerParameter(other.maxDownsampling);
    }

    @SetJIPipeDocumentation(name = "Noise type", description = "A predefined noise type or 'Custom' to setup custom values. \n\n" +
            "<ul>\n" +
            "    <li><strong>Poisson Noise Only (poisson)</strong>: poisson=0.8, blur=0.0, downsample=0.0, beta=0.7, gblur=1.0, iso</li>\n" +
            "    <li><strong>Poisson + Mild Blur (blur_expr)</strong>: poisson=0.8, blur=0.8, downsample=0.0, beta=0.1, gblur=0.5, iso</li>\n" +
            "    <li><strong>Poisson + Strong Blur (blur)</strong>: poisson=0.8, blur=0.8, downsample=0.0, beta=0.1, gblur=10.0, iso, uni</li>\n" +
            "    <li><strong>Poisson + Mild Downsample (downsample_expr)</strong>: poisson=0.8, blur=0.8, downsample=0.8, beta=0.03, gblur=1.0, iso</li>\n" +
            "    <li><strong>Poisson + Strong Downsample (downsample)</strong>: poisson=0.8, blur=0.8, downsample=0.8, beta=0.03, gblur=5.0, iso, uni</li>\n" +
            "    <li><strong>Poisson + Blur + Downsample (all)</strong>: poisson=[0.8, 0.8, 0.8], blur=[0.0, 0.8, 0.8], downsample=[0.0, 0.0, 0.8], beta=[0.7, 0.1, 0.03], gblur=[0.0, 10.0, 5.0], iso, uni</li>\n" +
            "    <li><strong>Poisson + Anisotropic Blur (aniso)</strong>: poisson=0.8, blur=0.8, downsample=0.8, beta=0.1, gblur=ds_max * 1.5</li>\n" +
            "</ul>\n")
    @JIPipeParameter(value = "noise-type", important = true, uiOrder = -99)
    public Cellpose3DenoiseTrainingNoiseType getNoiseType() {
        return noiseType;
    }

    @JIPipeParameter("noise-type")
    public void setNoiseType(Cellpose3DenoiseTrainingNoiseType noiseType) {
        this.noiseType = noiseType;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Poisson fraction of images", description = "Fraction of images to add poisson noise to")
    @JIPipeParameter("custom-poisson")
    public double getCustomPoisson() {
        return customPoisson;
    }

    @JIPipeParameter("custom-poisson")
    public void setCustomPoisson(double customPoisson) {
        this.customPoisson = customPoisson;
    }

    @SetJIPipeDocumentation(name = "Poisson Beta", description = "Scale of poisson noise")
    @JIPipeParameter("custom-beta")
    public double getCustomBeta() {
        return customBeta;
    }

    @JIPipeParameter("custom-beta")
    public void setCustomBeta(double customBeta) {
        this.customBeta = customBeta;
    }

    @SetJIPipeDocumentation(name = "Blur fraction of images", description = "Fraction of images to blur")
    @JIPipeParameter("custom-blur")
    public double getCustomBlur() {
        return customBlur;
    }

    @JIPipeParameter("custom-blur")
    public void setCustomBlur(double customBlur) {
        this.customBlur = customBlur;
    }

    @SetJIPipeDocumentation(name = "Blur Gaussian StdDev", description = "Scale of gaussian blurring stddev")
    @JIPipeParameter("custom-gblur")
    public double getCustomGBlur() {
        return customGBlur;
    }

    @JIPipeParameter("custom-gblur")
    public void setCustomGBlur(double customGBlur) {
        this.customGBlur = customGBlur;
    }

    @SetJIPipeDocumentation(name = "Downsample fraction of images", description = "Fraction of images to downsample")
    @JIPipeParameter("custom-downsample")
    public double getCustomDownsample() {
        return customDownsample;
    }

    @JIPipeParameter("custom-downsample")
    public void setCustomDownsample(double customDownsample) {
        this.customDownsample = customDownsample;
    }

    @SetJIPipeDocumentation(name = "Max downsampling factor", description = "The maximum downsampling factor")
    @JIPipeParameter("max-downsampling")
    public OptionalIntegerParameter getMaxDownsampling() {
        return maxDownsampling;
    }

    @JIPipeParameter("max-downsampling")
    public void setMaxDownsampling(OptionalIntegerParameter maxDownsampling) {
        this.maxDownsampling = maxDownsampling;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getKey().startsWith("custom-") && noiseType != Cellpose3DenoiseTrainingNoiseType.Custom) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }
}
