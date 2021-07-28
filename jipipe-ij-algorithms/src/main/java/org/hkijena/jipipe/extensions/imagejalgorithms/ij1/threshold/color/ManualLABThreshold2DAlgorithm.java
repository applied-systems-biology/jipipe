package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.LABBlueYellowTrackBackground;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.LABGreenRedTrackBackground;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.LABLightnessTrackBackground;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ColoredImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorLABData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.ranges.NumberRangeInvertedMode;
import org.hkijena.jipipe.extensions.parameters.ranges.NumberRangeParameterSettings;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Manual color threshold (LAB)", description = "Thresholds LAB images.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nColor")
@JIPipeInputSlot(value = ImagePlusColorLABData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
public class ManualLABThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntNumberRangeParameter lightnessThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter greenRedThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter blueYellowThreshold = new IntNumberRangeParameter(0, 256);
    private OptionalAnnotationNameParameter minLightnessThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold L", true);
    private OptionalAnnotationNameParameter maxLightnessThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold L", true);
    private OptionalAnnotationNameParameter minGreenRedThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold a", true);
    private OptionalAnnotationNameParameter maxGreenRedThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold a", true);
    private OptionalAnnotationNameParameter minBlueYellowThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold B", true);
    private OptionalAnnotationNameParameter maxBlueYellowThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold B", true);
    private JIPipeAnnotationMergeStrategy thresholdAnnotationStrategy = JIPipeAnnotationMergeStrategy.OverwriteExisting;


    public ManualLABThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ManualLABThreshold2DAlgorithm(ManualLABThreshold2DAlgorithm other) {
        super(other);
        this.lightnessThreshold = new IntNumberRangeParameter(other.lightnessThreshold);
        this.greenRedThreshold = new IntNumberRangeParameter(other.greenRedThreshold);
        this.blueYellowThreshold = new IntNumberRangeParameter(other.blueYellowThreshold);
        this.minLightnessThresholdAnnotation = new OptionalAnnotationNameParameter(other.minLightnessThresholdAnnotation);
        this.maxLightnessThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxLightnessThresholdAnnotation);
        this.minGreenRedThresholdAnnotation = new OptionalAnnotationNameParameter(other.minGreenRedThresholdAnnotation);
        this.maxGreenRedThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxGreenRedThresholdAnnotation);
        this.minBlueYellowThresholdAnnotation = new OptionalAnnotationNameParameter(other.minBlueYellowThresholdAnnotation);
        this.maxBlueYellowThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxBlueYellowThresholdAnnotation);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        if (!(dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo) instanceof ColoredImagePlusData)) {
            progressInfo.log("Info: Received an image without color space information! Its channels will be interpreted as LAB.");
        }
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusColorLABData.class, progressInfo).getImage();
        ImagePlus result = IJ.createHyperStack(img.getTitle() + " thresholded",
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                8);

        int minL = lightnessThreshold.getMin();
        int maxL = lightnessThreshold.getMax();
        int minA = greenRedThreshold.getMin();
        int maxA = greenRedThreshold.getMax();
        int minB = blueYellowThreshold.getMin();
        int maxB = blueYellowThreshold.getMax();

        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {

            ImageProcessor mask = ImageJUtils.getSliceZero(result, index);
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    int c = ip.get(x, y);
                    int l = (c & 0xff0000) >> 16;
                    int a = (c & 0xff00) >> 8;
                    int b = c & 0xff;

                    if (l < maxL && l >= minL && a < maxA && a >= minA && b < maxB && b >= minB) {
                        mask.set(x, y, 255);
                    }
                }
            }

        }, progressInfo);

        List<JIPipeAnnotation> annotations = new ArrayList<>();
        minLightnessThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minL);
        maxLightnessThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxL);
        minGreenRedThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minA);
        maxGreenRedThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxA);
        minBlueYellowThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minB);
        maxBlueYellowThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxB);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), annotations, thresholdAnnotationStrategy, progressInfo);
    }

    @JIPipeDocumentation(name = "L* threshold", description = "Thresholds the lightness channel (Channel 1)")
    @JIPipeParameter(value = "lab-l-threshold", uiOrder = -10)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = LABLightnessTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getLightnessThreshold() {
        return lightnessThreshold;
    }

    @JIPipeParameter("lab-l-threshold")
    public void setLightnessThreshold(IntNumberRangeParameter lightnessThreshold) {
        this.lightnessThreshold = lightnessThreshold;
    }

    @JIPipeDocumentation(name = "a* threshold", description = "Thresholds the green-red/a* channel (Channel 2)")
    @JIPipeParameter(value = "lab-a-threshold", uiOrder = -9)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = LABGreenRedTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getGreenRedThreshold() {
        return greenRedThreshold;
    }

    @JIPipeParameter("lab-a-threshold")
    public void setGreenRedThreshold(IntNumberRangeParameter greenRedThreshold) {
        this.greenRedThreshold = greenRedThreshold;
    }

    @JIPipeDocumentation(name = "b* threshold", description = "Thresholds the blue-yellow/b* channel (Channel 3)")
    @JIPipeParameter(value = "lab-b-threshold", uiOrder = -8)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = LABBlueYellowTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getBlueYellowThreshold() {
        return blueYellowThreshold;
    }

    @JIPipeParameter("lab-b-threshold")
    public void setBlueYellowThreshold(IntNumberRangeParameter blueYellowThreshold) {
        this.blueYellowThreshold = blueYellowThreshold;
    }

    @JIPipeDocumentation(name = "Min threshold annotation (R)", description = "If enabled, annotate with the min red threshold")
    @JIPipeParameter("annotate-min-red")
    public OptionalAnnotationNameParameter getMinLightnessThresholdAnnotation() {
        return minLightnessThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-red")
    public void setMinLightnessThresholdAnnotation(OptionalAnnotationNameParameter minLightnessThresholdAnnotation) {
        this.minLightnessThresholdAnnotation = minLightnessThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation (R)", description = "If enabled, annotate with the max red threshold")
    @JIPipeParameter("annotate-max-red")
    public OptionalAnnotationNameParameter getMaxLightnessThresholdAnnotation() {
        return maxLightnessThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-red")
    public void setMaxLightnessThresholdAnnotation(OptionalAnnotationNameParameter maxLightnessThresholdAnnotation) {
        this.maxLightnessThresholdAnnotation = maxLightnessThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Min threshold annotation (G)", description = "If enabled, annotate with the min green threshold")
    @JIPipeParameter("annotate-min-green")
    public OptionalAnnotationNameParameter getMinGreenRedThresholdAnnotation() {
        return minGreenRedThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-green")
    public void setMinGreenRedThresholdAnnotation(OptionalAnnotationNameParameter minGreenRedThresholdAnnotation) {
        this.minGreenRedThresholdAnnotation = minGreenRedThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation (G)", description = "If enabled, annotate with the max green threshold")
    @JIPipeParameter("annotate-max-green")
    public OptionalAnnotationNameParameter getMaxGreenRedThresholdAnnotation() {
        return maxGreenRedThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-green")
    public void setMaxGreenRedThresholdAnnotation(OptionalAnnotationNameParameter maxGreenRedThresholdAnnotation) {
        this.maxGreenRedThresholdAnnotation = maxGreenRedThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Min threshold annotation (B)", description = "If enabled, annotate with the min blue threshold")
    @JIPipeParameter("annotate-min-blue")
    public OptionalAnnotationNameParameter getMinBlueYellowThresholdAnnotation() {
        return minBlueYellowThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-blue")
    public void setMinBlueYellowThresholdAnnotation(OptionalAnnotationNameParameter minBlueYellowThresholdAnnotation) {
        this.minBlueYellowThresholdAnnotation = minBlueYellowThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Max threshold annotation (B)", description = "If enabled, annotate with the max blue threshold")
    @JIPipeParameter("annotate-max-blue")
    public OptionalAnnotationNameParameter getMaxBlueYellowThresholdAnnotation() {
        return maxBlueYellowThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-blue")
    public void setMaxBlueYellowThresholdAnnotation(OptionalAnnotationNameParameter maxBlueYellowThresholdAnnotation) {
        this.maxBlueYellowThresholdAnnotation = maxBlueYellowThresholdAnnotation;
    }

    @JIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeAnnotationMergeStrategy getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeAnnotationMergeStrategy thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }
}
