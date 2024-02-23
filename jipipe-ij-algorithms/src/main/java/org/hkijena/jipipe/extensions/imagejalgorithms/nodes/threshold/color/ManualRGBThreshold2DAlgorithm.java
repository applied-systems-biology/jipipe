package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold.color;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.RGBBlueTrackBackground;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.RGBGreenTrackBackground;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.RGBRedTrackBackground;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.ranges.IntNumberRangeParameter;
import org.hkijena.jipipe.extensions.parameters.library.ranges.NumberRangeInvertedMode;
import org.hkijena.jipipe.extensions.parameters.library.ranges.NumberRangeParameterSettings;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Manual color threshold (RGB)", description = "Thresholds RGB images.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Threshold\nColor")
@AddJIPipeInputSlot(value = ImagePlusColorRGBData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust")
public class ManualRGBThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntNumberRangeParameter redThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter blueThreshold = new IntNumberRangeParameter(0, 256);
    private IntNumberRangeParameter greenThreshold = new IntNumberRangeParameter(0, 256);
    private OptionalAnnotationNameParameter minRedThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold R", true);
    private OptionalAnnotationNameParameter maxRedThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold R", true);
    private OptionalAnnotationNameParameter minGreenThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold G", true);
    private OptionalAnnotationNameParameter maxGreenThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold G", true);
    private OptionalAnnotationNameParameter minBlueThresholdAnnotation = new OptionalAnnotationNameParameter("Min Threshold B", true);
    private OptionalAnnotationNameParameter maxBlueThresholdAnnotation = new OptionalAnnotationNameParameter("Max Threshold B", true);
    private JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;


    public ManualRGBThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ManualRGBThreshold2DAlgorithm(ManualRGBThreshold2DAlgorithm other) {
        super(other);
        this.redThreshold = new IntNumberRangeParameter(other.redThreshold);
        this.blueThreshold = new IntNumberRangeParameter(other.blueThreshold);
        this.greenThreshold = new IntNumberRangeParameter(other.greenThreshold);
        this.minRedThresholdAnnotation = new OptionalAnnotationNameParameter(other.minRedThresholdAnnotation);
        this.maxRedThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxRedThresholdAnnotation);
        this.minGreenThresholdAnnotation = new OptionalAnnotationNameParameter(other.minGreenThresholdAnnotation);
        this.maxGreenThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxGreenThresholdAnnotation);
        this.minBlueThresholdAnnotation = new OptionalAnnotationNameParameter(other.minBlueThresholdAnnotation);
        this.maxBlueThresholdAnnotation = new OptionalAnnotationNameParameter(other.maxBlueThresholdAnnotation);
        this.thresholdAnnotationStrategy = other.thresholdAnnotationStrategy;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusColorRGBData.class, progressInfo).getImage();
        ImagePlus result = IJ.createHyperStack(img.getTitle() + " thresholded",
                img.getWidth(),
                img.getHeight(),
                img.getNChannels(),
                img.getNSlices(),
                img.getNFrames(),
                8);

        int minR = redThreshold.getMin();
        int maxR = redThreshold.getMax();
        int minG = greenThreshold.getMin();
        int maxG = greenThreshold.getMax();
        int minB = blueThreshold.getMin();
        int maxB = blueThreshold.getMax();

        ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {

            ImageProcessor mask = ImageJUtils.getSliceZero(result, index);
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    int c = ip.get(x, y);
                    int r = (c & 0xff0000) >> 16;
                    int g = (c & 0xff00) >> 8;
                    int b = c & 0xff;

                    if (r < maxR && r >= minR && g < maxG && g >= minG && b < maxB && b >= minB) {
                        mask.set(x, y, 255);
                    }
                }
            }

        }, progressInfo);

        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        minRedThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minR);
        maxRedThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxR);
        minGreenThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minG);
        maxGreenThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxG);
        minBlueThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + minB);
        maxBlueThresholdAnnotation.addAnnotationIfEnabled(annotations, "" + maxB);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), annotations, thresholdAnnotationStrategy, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Red threshold", description = "Thresholds the red channel (Channel 1)")
    @JIPipeParameter(value = "red-threshold", uiOrder = -10)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = RGBRedTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getRedThreshold() {
        return redThreshold;
    }

    @JIPipeParameter("red-threshold")
    public void setRedThreshold(IntNumberRangeParameter redThreshold) {
        this.redThreshold = redThreshold;
    }

    @SetJIPipeDocumentation(name = "Green threshold", description = "Thresholds the green channel (Channel 2)")
    @JIPipeParameter(value = "green-threshold", uiOrder = -9)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = RGBGreenTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getGreenThreshold() {
        return greenThreshold;
    }

    @JIPipeParameter("green-threshold")
    public void setGreenThreshold(IntNumberRangeParameter greenThreshold) {
        this.greenThreshold = greenThreshold;
    }

    @SetJIPipeDocumentation(name = "Blue threshold", description = "Thresholds the blue channel (Channel 3)")
    @JIPipeParameter(value = "blue-threshold", uiOrder = -8)
    @NumberRangeParameterSettings(min = 0, max = 256, trackBackground = RGBBlueTrackBackground.class, invertedMode = NumberRangeInvertedMode.OutsideMinMax)
    public IntNumberRangeParameter getBlueThreshold() {
        return blueThreshold;
    }

    @JIPipeParameter("blue-threshold")
    public void setBlueThreshold(IntNumberRangeParameter blueThreshold) {
        this.blueThreshold = blueThreshold;
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation (R)", description = "If enabled, annotate with the min red threshold")
    @JIPipeParameter("annotate-min-red")
    public OptionalAnnotationNameParameter getMinRedThresholdAnnotation() {
        return minRedThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-red")
    public void setMinRedThresholdAnnotation(OptionalAnnotationNameParameter minRedThresholdAnnotation) {
        this.minRedThresholdAnnotation = minRedThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation (R)", description = "If enabled, annotate with the max red threshold")
    @JIPipeParameter("annotate-max-red")
    public OptionalAnnotationNameParameter getMaxRedThresholdAnnotation() {
        return maxRedThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-red")
    public void setMaxRedThresholdAnnotation(OptionalAnnotationNameParameter maxRedThresholdAnnotation) {
        this.maxRedThresholdAnnotation = maxRedThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation (G)", description = "If enabled, annotate with the min green threshold")
    @JIPipeParameter("annotate-min-green")
    public OptionalAnnotationNameParameter getMinGreenThresholdAnnotation() {
        return minGreenThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-green")
    public void setMinGreenThresholdAnnotation(OptionalAnnotationNameParameter minGreenThresholdAnnotation) {
        this.minGreenThresholdAnnotation = minGreenThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation (G)", description = "If enabled, annotate with the max green threshold")
    @JIPipeParameter("annotate-max-green")
    public OptionalAnnotationNameParameter getMaxGreenThresholdAnnotation() {
        return maxGreenThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-green")
    public void setMaxGreenThresholdAnnotation(OptionalAnnotationNameParameter maxGreenThresholdAnnotation) {
        this.maxGreenThresholdAnnotation = maxGreenThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Min threshold annotation (B)", description = "If enabled, annotate with the min blue threshold")
    @JIPipeParameter("annotate-min-blue")
    public OptionalAnnotationNameParameter getMinBlueThresholdAnnotation() {
        return minBlueThresholdAnnotation;
    }

    @JIPipeParameter("annotate-min-blue")
    public void setMinBlueThresholdAnnotation(OptionalAnnotationNameParameter minBlueThresholdAnnotation) {
        this.minBlueThresholdAnnotation = minBlueThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Max threshold annotation (B)", description = "If enabled, annotate with the max blue threshold")
    @JIPipeParameter("annotate-max-blue")
    public OptionalAnnotationNameParameter getMaxBlueThresholdAnnotation() {
        return maxBlueThresholdAnnotation;
    }

    @JIPipeParameter("annotate-max-blue")
    public void setMaxBlueThresholdAnnotation(OptionalAnnotationNameParameter maxBlueThresholdAnnotation) {
        this.maxBlueThresholdAnnotation = maxBlueThresholdAnnotation;
    }

    @SetJIPipeDocumentation(name = "Threshold annotation strategy", description = "Determines what happens if annotations are already present.")
    @JIPipeParameter("threshold-annotation-strategy")
    public JIPipeTextAnnotationMergeMode getThresholdAnnotationStrategy() {
        return thresholdAnnotationStrategy;
    }

    @JIPipeParameter("threshold-annotation-strategy")
    public void setThresholdAnnotationStrategy(JIPipeTextAnnotationMergeMode thresholdAnnotationStrategy) {
        this.thresholdAnnotationStrategy = thresholdAnnotationStrategy;
    }
}
