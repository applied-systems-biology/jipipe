package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import ij.IJ;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Split labels into individual images", description = "Splits all labels from a labels image into their own image. Can be reverted with Image calculator 2D (merge).")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Labels")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
public class SeparateLabelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter labelIndexAnnotation = new OptionalTextAnnotationNameParameter("Label", true);

    public SeparateLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SeparateLabelsAlgorithm(SeparateLabelsAlgorithm other) {
        super(other);
        this.labelIndexAnnotation = new OptionalTextAnnotationNameParameter(other.labelIndexAnnotation);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        int[] allLabels = LabelImages.findAllLabels(img);
        for (int i = 0; i < allLabels.length; i++) {
            JIPipeProgressInfo labelProgress = progressInfo.resolveAndLog("Label " + allLabels[i], i, allLabels.length);
            final int label = allLabels[i];
            ImagePlus result = img.duplicate();
            result.setTitle(img.getTitle() + " label=" + label);
            ImageJUtils.forEachIndexedZCTSlice(result, (ip, index ) -> {
                ImageJAlgorithmUtils.removeLabelsExcept(ip, new int[] { label });
            }, labelProgress);
            result.copyScale(img);
            List<JIPipeTextAnnotation> annotationList = new ArrayList<>();
            labelIndexAnnotation.addAnnotationIfEnabled(annotationList, String.valueOf(label));
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(result), annotationList, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Label index", description = "If enabled, annotate the resulting images with the label index")
    @JIPipeParameter("label-index")
    public OptionalTextAnnotationNameParameter getLabelIndexAnnotation() {
        return labelIndexAnnotation;
    }

    @JIPipeParameter("label-index")
    public void setLabelIndexAnnotation(OptionalTextAnnotationNameParameter labelIndexAnnotation) {
        this.labelIndexAnnotation = labelIndexAnnotation;
    }
}
