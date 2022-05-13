package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.graph.OutputSlotMapParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Split by dimension", description = "Splits a hyperstack by a selected dimension into a single or multiple output slots. " +
        "Multiple output slots can be utilized to split specific indices.")
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true, inheritedSlot = "Input")
public class SplitByDimensionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OutputSlotMapParameterCollection outputIndices;
    private HyperstackDimension targetDimension = HyperstackDimension.Channel;
    private JIPipeTextAnnotationMergeMode annotationMergeStrategy = JIPipeTextAnnotationMergeMode.OverwriteExisting;
    private OptionalAnnotationNameParameter targetDimensionAnnotation = new OptionalAnnotationNameParameter("Channel", true);

    public SplitByDimensionAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", ImagePlusData.class)
                .addOutputSlot("Output", "", ImagePlusData.class, "Input")
                .sealInput()
                .build());
        this.outputIndices = new OutputSlotMapParameterCollection(OptionalIntegerRange.class, this, null, false);
        outputIndices.updateSlots();
        registerSubParameter(outputIndices);
    }

    public SplitByDimensionAlgorithm(SplitByDimensionAlgorithm other) {
        super(other);

        this.targetDimension = other.targetDimension;
        this.targetDimensionAnnotation = new OptionalAnnotationNameParameter(other.targetDimensionAnnotation);
        outputIndices = new OutputSlotMapParameterCollection(OptionalIntegerRange.class, this, null, false);
        other.outputIndices.copyTo(outputIndices);
        registerSubParameter(outputIndices);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        if (targetDimension == HyperstackDimension.Channel) {
            for (int c = 0; c < img.getNChannels(); c++) {
                JIPipeProgressInfo stackProgressInfo = progressInfo.resolveAndLog("Output channel", c, img.getNChannels());
                ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getNFrames() * img.getNSlices());
                int finalC = c;
                ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    if (index.getC() != finalC)
                        return;
                    int stackIndex = ImageJUtils.oneSliceIndexToOneStackIndex(1, index.getZ() + 1, index.getT() + 1, 1, img.getNSlices(), img.getNFrames());
                    stack.setProcessor(ip, stackIndex);
                }, stackProgressInfo);
                ImagePlus stackOutput = new ImagePlus(img.getTitle(), stack);
                stackOutput.setDimensions(1, img.getNSlices(), img.getNFrames());
                stackOutput.copyScale(img);

                List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
                targetDimensionAnnotation.addAnnotationIfEnabled(annotationList, c + "");

                for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                    OptionalIntegerRange range = outputIndices.get(outputSlot.getName()).get(OptionalIntegerRange.class);
                    if (range.isEnabled()) {
                        if (!range.getContent().getIntegers(0, img.getNChannels()).contains(c)) {
                            continue;
                        }
                    }
                    dataBatch.addOutputData(outputSlot, new ImagePlusData(stackOutput), annotationList, annotationMergeStrategy, stackProgressInfo);
                }
            }
        } else if (targetDimension == HyperstackDimension.Depth) {
            for (int z = 0; z < img.getNSlices(); z++) {
                JIPipeProgressInfo stackProgressInfo = progressInfo.resolveAndLog("Output slice", z, img.getNSlices());
                ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getNFrames() * img.getNChannels());
                int finalZ = z;
                ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    if (index.getZ() != finalZ)
                        return;
                    int stackIndex = ImageJUtils.oneSliceIndexToOneStackIndex(1 + index.getC(), 1, index.getT() + 1, img.getNChannels(), 1, img.getNFrames());
                    stack.setProcessor(ip, stackIndex);
                }, stackProgressInfo);
                ImagePlus stackOutput = new ImagePlus(img.getTitle(), stack);
                stackOutput.setDimensions(img.getNChannels(), 1, img.getNFrames());
                stackOutput.copyScale(img);

                List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
                targetDimensionAnnotation.addAnnotationIfEnabled(annotationList, z + "");

                for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                    OptionalIntegerRange range = outputIndices.get(outputSlot.getName()).get(OptionalIntegerRange.class);
                    if (range.isEnabled()) {
                        if (!range.getContent().getIntegers(0, img.getNSlices()).contains(z)) {
                            continue;
                        }
                    }
                    dataBatch.addOutputData(outputSlot, new ImagePlusData(stackOutput), annotationList, annotationMergeStrategy, stackProgressInfo);
                }
            }
        } else if (targetDimension == HyperstackDimension.Frame) {
            for (int t = 0; t < img.getNFrames(); t++) {
                JIPipeProgressInfo stackProgressInfo = progressInfo.resolveAndLog("Output slice", t, img.getNFrames());
                ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getNSlices() * img.getNChannels());
                int finalT = t;
                ImageJUtils.forEachIndexedZCTSlice(img, (ip, index) -> {
                    if (index.getT() != finalT)
                        return;
                    int stackIndex = ImageJUtils.oneSliceIndexToOneStackIndex(1 + index.getC(), 1 + index.getZ(), 1, img.getNChannels(), img.getNSlices(), 1);
                    stack.setProcessor(ip, stackIndex);
                }, stackProgressInfo);
                ImagePlus stackOutput = new ImagePlus(img.getTitle(), stack);
                stackOutput.setDimensions(img.getNChannels(), img.getNSlices(), 1);
                stackOutput.copyScale(img);

                List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
                targetDimensionAnnotation.addAnnotationIfEnabled(annotationList, t + "");

                for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                    OptionalIntegerRange range = outputIndices.get(outputSlot.getName()).get(OptionalIntegerRange.class);
                    if (range.isEnabled()) {
                        if (!range.getContent().getIntegers(0, img.getNFrames()).contains(t)) {
                            continue;
                        }
                    }
                    dataBatch.addOutputData(outputSlot, new ImagePlusData(stackOutput), annotationList, annotationMergeStrategy, stackProgressInfo);
                }
            }
        }

    }

    @JIPipeDocumentation(name = "Split into output slots", description = "Following settings allow you to determine which generated data is put in which output slot. " +
            "Enable the range parameter and set the range of indices (zero being the first). Example: 0-10;15-21")
    @JIPipeParameter("output-indices")
    public OutputSlotMapParameterCollection getOutputIndices() {
        return outputIndices;
    }

    @JIPipeDocumentation(name = "Split dimension", description = "Determines by which dimension the incoming stacks are split.")
    @JIPipeParameter("target-dimension")
    public HyperstackDimension getTargetDimension() {
        return targetDimension;
    }

    @JIPipeParameter("target-dimension")
    public void setTargetDimension(HyperstackDimension targetDimension) {
        this.targetDimension = targetDimension;
    }

    @JIPipeDocumentation(name = "Annotate with split dimension", description = "If enabled, create annotations for the index of the split dimension.")
    @JIPipeParameter("target-dimension-annotation")
    public OptionalAnnotationNameParameter getTargetDimensionAnnotation() {
        return targetDimensionAnnotation;
    }

    @JIPipeParameter("target-dimension-annotation")
    public void setTargetDimensionAnnotation(OptionalAnnotationNameParameter targetDimensionAnnotation) {
        this.targetDimensionAnnotation = targetDimensionAnnotation;
    }

    @JIPipeDocumentation(name = "Annotation merge strategy", description = "Determines how annotations are overwritten.")
    @JIPipeParameter("annotation-merge-strategy")
    public JIPipeTextAnnotationMergeMode getAnnotationMergeStrategy() {
        return annotationMergeStrategy;
    }

    @JIPipeParameter("annotation-merge-strategy")
    public void setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode annotationMergeStrategy) {
        this.annotationMergeStrategy = annotationMergeStrategy;
    }
}
