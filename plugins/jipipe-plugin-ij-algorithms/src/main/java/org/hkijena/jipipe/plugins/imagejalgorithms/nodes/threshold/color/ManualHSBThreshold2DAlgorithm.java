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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.BlackToWhiteTrackBackground;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.HSBHueTrackBackground;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.WhiteToRedTrackBackground;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.plugins.parameters.library.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.plugins.parameters.library.ranges.NumberRangeInvertedMode;
import org.hkijena.jipipe.plugins.parameters.library.ranges.NumberRangeParameterSettings;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Manual color threshold (HSB)", description = "Thresholds HSB images.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nColor")
@AddJIPipeInputSlot(value = ImagePlusColorHSBData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust")
public class ManualHSBThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntNumberRangeParameter hueThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter brightnessThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter saturationThreshold = new IntNumberRangeParameter(0, 256);
    private OptionalTextAnnotationNameParameter minHueThresholdAnnotation = new OptionalTextAnnotationNameParameter("Min Threshold H", false);
    private OptionalTextAnnotationNameParameter maxHueThresholdAnnotation = new OptionalTextAnnotationNameParameter("Max Threshold H", false);
    private OptionalTextAnnotationNameParameter minBrightnessThresholdAnnotation = new OptionalTextAnnotationNameParameter("Min Threshold B", false);
    private OptionalTextAnnotationNameParameter maxBrightnessThresholdAnnotation = new OptionalTextAnnotationNameParameter("Max Threshold B", false);
    private OptionalTextAnnotationNameParameter minSaturationThresholdAnnotation = new OptionalTextAnnotationNameParameter("Min Threshold S", false);
    private OptionalTextAnnotationNameParameter maxSaturationThresholdAnnotation = new OptionalTextAnnotationNameParameter("Max Threshold S", false);
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;


    public ManualHSBThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ManualHSBThreshold2DAlgorithm(ManualHSBThreshold2DAlgorithm other) {
        super(other);
        this.hueThreshold = new IntNumberRangeParameter(other.hueThreshold);
        this.brightnessThreshold = new IntNumberRangeParameter(other.brightnessThreshold);
        this.saturationThreshold = new IntNumberRangeParameter(other.saturationThreshold);
        this.minHueThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.minHueThresholdAnnotation);
        this.maxHueThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.maxHueThresholdAnnotation);
        this.minBrightnessThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.minBrightnessThresholdAnnotation);
        this.maxBrightnessThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.maxBrightnessThresholdAnnotation);
        this.minSaturationThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.minSaturationThresholdAnnotation);
        this.maxSaturationThresholdAnnotation = new OptionalTextAnnotationNameParameter(other.maxSaturationThresholdAnnotation);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusColorHSBData.class, progressInfo).getImage();
        ImagePlus result = IJ.createHyperStack(img.getTitle() + " thresholded",
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                8);

        int minH = hueThreshold.getMin();
        int maxH = hueThreshold.getMax();
        int minS = saturationThreshold.getMin();
        int maxS = saturationThreshold.getMax();
        int minB = brightnessThreshold.getMin();
        int maxB = brightnessThreshold.getMax();

        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {

            ImageProcessor mask = ImageJUtils.getSliceZero(result, index);
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    int c = ip.get(x, y);
                    int H = (c & 0xff0000) >> 16;
                    int S = (c & 0xff00) >> 8;
                    int B = c & 0xff;

                    if (H < maxH && H >= minH && S < maxS && S >= minS && B < maxB && B >= minB) {
                        mask.set(x, y, 255);
                    }
                }
            }

        }, progressInfo);

        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        minHueThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minH);
        maxHueThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxH);
        minSaturationThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minS);
        maxSaturationThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxS);
        minBrightnessThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minB);
        maxBrightnessThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxB);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), annotations, thresholdAnnotationStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Hue threshold", description = "Thresholds the hue channel (Channel 1)")
    @JIPipeParameter(value = "hue-threshold", uiOrder = -10)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = HSBHueTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getHueThreshold() {
        return hueThreshold;
    }

    @JIPipeParameter("hue-threshold")
    public void setHueThreshold(IntNumberRangeParameter hueThreshold) {
        this.hueThreshold = hueThreshold;
    }

    @SetJIPipeDocumentation(name = "Brightness threshold", description = "Thresholds the brightness channel (Channel 3)")
    @JIPipeParameter(value = "brightness-threshold", uiOrder = -8)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = BlackToWhiteTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getBrightnessThreshold() {
        return brightnessThreshold;
    }

    @JIPipeParameter("brightness-threshold")
    public void setBrightnessThreshold(IntNumberRangeParameter brightnessThreshold) {
        this.brightnessThreshold = brightnessThreshold;
    }

    @SetJIPipeDocumentation(name = "Saturation threshold", description = "Thresholds the saturation channel (Channel 2)")
    @JIPipeParameter(value = "saturation-threshold", uiOrder = -9)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = WhiteToRedTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getSaturationThreshold() {
        return saturationThreshold;
    }

    @JIPipeParameter("saturation-threshold")
    public void setSaturationThreshold(IntNumberRangeParameter saturationThreshold) {
        this.saturationThreshold = saturationThreshold;
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation (H)", description = "If enabled, annotate with the min hue threshold")
    @JIPipeParameter("annotate-min-hue")
    public OptionalTextAnnotationNameParameter getMinHueThresholdAnnotation() {
        return minHueThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-hue")
    public void setMinHueThresholdAnnotation(OptionalTextAnnotationNameParameter minHueThresholdAnnotation) {
        this.minHueThresholdAnnotation = minHueThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation (H)", description = "If enabled, annotate with the max hue threshold")
    @JIPipeParameter("annotate-max-hue")
    public OptionalTextAnnotationNameParameter getMaxHueThresholdAnnotation() {
        return maxHueThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-hue")
    public void setMaxHueThresholdAnnotation(OptionalTextAnnotationNameParameter maxHueThresholdAnnotation) {
        this.maxHueThresholdAnnotation = maxHueThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation (B)", description = "If enabled, annotate with the min brightness threshold")
    @JIPipeParameter("annotate-min-brightness")
    public OptionalTextAnnotationNameParameter getMinBrightnessThresholdAnnotation() {
        return minBrightnessThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-brightness")
    public void setMinBrightnessThresholdAnnotation(OptionalTextAnnotationNameParameter minBrightnessThresholdAnnotation) {
        this.minBrightnessThresholdAnnotation = minBrightnessThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation (B)", description = "If enabled, annotate with the max brightness threshold")
    @JIPipeParameter("annotate-max-brightness")
    public OptionalTextAnnotationNameParameter getMaxBrightnessThresholdAnnotation() {
        return maxBrightnessThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-brightness")
    public void setMaxBrightnessThresholdAnnotation(OptionalTextAnnotationNameParameter maxBrightnessThresholdAnnotation) {
        this.maxBrightnessThresholdAnnotation = maxBrightnessThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation (S)", description = "If enabled, annotate with the min saturation threshold")
    @JIPipeParameter("annotate-min-saturation")
    public OptionalTextAnnotationNameParameter getMinSaturationThresholdAnnotation() {
        return minSaturationThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-saturation")
    public void setMinSaturationThresholdAnnotation(OptionalTextAnnotationNameParameter minSaturationThresholdAnnotation) {
        this.minSaturationThresholdAnnotation = minSaturationThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation (S)", description = "If enabled, annotate with the max saturation threshold")
    @JIPipeParameter("annotate-max-saturation")
    public OptionalTextAnnotationNameParameter getMaxSaturationThresholdAnnotation() {
        return maxSaturationThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-saturation")
    public void setMaxSaturationThresholdAnnotation(OptionalTextAnnotationNameParameter maxSaturationThresholdAnnotation) {
        this.maxSaturationThresholdAnnotation = maxSaturationThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Threshold annotation mode", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }
}
