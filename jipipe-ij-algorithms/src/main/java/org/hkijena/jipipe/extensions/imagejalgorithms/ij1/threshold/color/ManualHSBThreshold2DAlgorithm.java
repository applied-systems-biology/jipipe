package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.BlackToWhiteTrackBackground;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.HSBHueTrackBackground;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.WhiteToRedTrackBackground;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorHSBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.ranges.NumberRangeInvertedMode;
import org.hkijena.jipipe.extensions.parameters.ranges.NumberRangeParameterSettings;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Manual color threshold (HSB)", description = "Thresholds HSB images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nColor")
@JIPipeInputSlot(value = ImagePlusColorHSBData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
public class ManualHSBThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntNumberRangeParameter hueThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter brightnessThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter saturationThreshold = new IntNumberRangeParameter(0, 256);
    private OptionalAnnotationNameParameter minHueThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold H", true);
    private OptionalAnnotationNameParameter maxHueThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold H", true);
    private OptionalAnnotationNameParameter minBrightnessThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold B", true);
    private OptionalAnnotationNameParameter maxBrightnessThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold B", true);
    private OptionalAnnotationNameParameter minSaturationThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold S", true);
    private OptionalAnnotationNameParameter maxSaturationThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold S", true);
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;


    public ManualHSBThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ManualHSBThreshold2DAlgorithm(ManualHSBThreshold2DAlgorithm other) {
        super(other);
        this.hueThreshold = new IntNumberRangeParameter(other.hueThreshold);
        this.brightnessThreshold = new IntNumberRangeParameter(other.brightnessThreshold);
        this.saturationThreshold = new IntNumberRangeParameter(other.saturationThreshold);
        this.minHueThresholdAnnotation = new OptionalAnnotationNameParameter(other.minHueThresholdAnnotation);
        this.maxHueThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxHueThresholdAnnotation);
        this.minBrightnessThresholdAnnotation = new OptionalAnnotationNameParameter(other.minBrightnessThresholdAnnotation);
        this.maxBrightnessThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxBrightnessThresholdAnnotation);
        this.minSaturationThresholdAnnotation = new OptionalAnnotationNameParameter(other.minSaturationThresholdAnnotation);
        this.maxSaturationThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxSaturationThresholdAnnotation);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (!(dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo) instanceof ColoredImagePlusData)) {
            progressInfo.log("Info: Received an image without color space information! Its channels will be interpreted as HSB.");
        }
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusColorHSBData.class, progressInfo).getImage();
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

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), annotations, thresholdAnnotationStrategy, progressInfo);
    }

    @JIPipeDocumentation(name = "Hue threshold", description = "Thresholds the hue channel (Channel 1)")
    @JIPipeParameter(value = "hue-threshold", uiOrder = -10)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = HSBHueTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getHueThreshold() {
        return hueThreshold;
    }

    @JIPipeParameter("hue-threshold")
    public void setHueThreshold(IntNumberRangeParameter hueThreshold) {
        this.hueThreshold = hueThreshold;
    }

    @JIPipeDocumentation(name = "Brightness threshold", description = "Thresholds the brightness channel (Channel 3)")
    @JIPipeParameter(value = "brightness-threshold", uiOrder = -8)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = BlackToWhiteTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getBrightnessThreshold() {
        return brightnessThreshold;
    }

    @JIPipeParameter("brightness-threshold")
    public void setBrightnessThreshold(IntNumberRangeParameter brightnessThreshold) {
        this.brightnessThreshold = brightnessThreshold;
    }

    @JIPipeDocumentation(name = "Saturation threshold", description = "Thresholds the saturation channel (Channel 2)")
    @JIPipeParameter(value = "saturation-threshold", uiOrder = -9)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = WhiteToRedTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getSaturationThreshold() {
        return saturationThreshold;
    }

    @JIPipeParameter("saturation-threshold")
    public void setSaturationThreshold(IntNumberRangeParameter saturationThreshold) {
        this.saturationThreshold = saturationThreshold;
    }

    @JIPipeDocumentation(name = "Min threshold annotation (H)", description = "If enabled, annotate with the min hue threshold")
    @JIPipeParameter("annotate-min-hue")
    public OptionalAnnotationNameParameter getMinHueThresholdAnnotation() {
        return minHueThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-hue")
    public void setMinHueThresholdAnnotation(OptionalAnnotationNameParameter minHueThresholdAnnotation) {
        this.minHueThresholdAnnotation = minHueThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation (H)", description = "If enabled, annotate with the max hue threshold")
    @JIPipeParameter("annotate-max-hue")
    public OptionalAnnotationNameParameter getMaxHueThresholdAnnotation() {
        return maxHueThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-hue")
    public void setMaxHueThresholdAnnotation(OptionalAnnotationNameParameter maxHueThresholdAnnotation) {
        this.maxHueThresholdAnnotation = maxHueThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Min threshold annotation (B)", description = "If enabled, annotate with the min brightness threshold")
    @JIPipeParameter("annotate-min-brightness")
    public OptionalAnnotationNameParameter getMinBrightnessThresholdAnnotation() {
        return minBrightnessThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-brightness")
    public void setMinBrightnessThresholdAnnotation(OptionalAnnotationNameParameter minBrightnessThresholdAnnotation) {
        this.minBrightnessThresholdAnnotation = minBrightnessThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation (B)", description = "If enabled, annotate with the max brightness threshold")
    @JIPipeParameter("annotate-max-brightness")
    public OptionalAnnotationNameParameter getMaxBrightnessThresholdAnnotation() {
        return maxBrightnessThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-brightness")
    public void setMaxBrightnessThresholdAnnotation(OptionalAnnotationNameParameter maxBrightnessThresholdAnnotation) {
        this.maxBrightnessThresholdAnnotation = maxBrightnessThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Min threshold annotation (S)", description = "If enabled, annotate with the min saturation threshold")
    @JIPipeParameter("annotate-min-saturation")
    public OptionalAnnotationNameParameter getMinSaturationThresholdAnnotation() {
        return minSaturationThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-saturation")
    public void setMinSaturationThresholdAnnotation(OptionalAnnotationNameParameter minSaturationThresholdAnnotation) {
        this.minSaturationThresholdAnnotation = minSaturationThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation (S)", description = "If enabled, annotate with the max saturation threshold")
    @JIPipeParameter("annotate-max-saturation")
    public OptionalAnnotationNameParameter getMaxSaturationThresholdAnnotation() {
        return maxSaturationThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-saturation")
    public void setMaxSaturationThresholdAnnotation(OptionalAnnotationNameParameter maxSaturationThresholdAnnotation) {
        this.maxSaturationThresholdAnnotation = maxSaturationThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }
}
