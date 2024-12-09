package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.LineMirror;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImagePlusPropertiesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

@SetJIPipeDocumentation(name = "Mirror image over line 2D (ROI)", description = "The two endpoints of a line a provided via one or multiple line ROI. " +
        "If multiple ROI are provided, the algorithm is executed per ROI. " +
        "If a ROI is not a line ROI, a line outline/hull operation is applied to the ROI (see Outline 2D ROI)." +
        "The resulting line is used as axis to mirror the image pixels. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Mirror", create = true, role = JIPipeDataSlotRole.ParametersLooping)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
public class LineMirror2DFromRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private LineMirror.MirrorOperationMode mode = LineMirror.MirrorOperationMode.Max;
    private OptionalTextAnnotationNameParameter annotateWithRoiIndex = new OptionalTextAnnotationNameParameter("ROI index", false);
    private OptionalTextAnnotationNameParameter annotateWithRoiName = new OptionalTextAnnotationNameParameter("ROI name", false);

    public LineMirror2DFromRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LineMirror2DFromRoiAlgorithm(LineMirror2DFromRoiAlgorithm other) {
        super(other);
        this.mode = other.mode;
        this.annotateWithRoiIndex = new OptionalTextAnnotationNameParameter(other.annotateWithRoiIndex);
        this.annotateWithRoiName = new OptionalTextAnnotationNameParameter(other.annotateWithRoiName);
    }

    @SetJIPipeDocumentation(name = "Mode", description = "The way how the mirror operation should be applied given the two pixel values. " +
            "You can either set both pixels to the min/max value (per channel on RGB) or only copy data from a specific side.")
    @JIPipeParameter("mode")
    public LineMirror.MirrorOperationMode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(LineMirror.MirrorOperationMode mode) {
        this.mode = mode;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI index", description = "If enabled, annotate the output image with the mirror ROI index")
    @JIPipeParameter("annotate-with-roi-index")
    public OptionalTextAnnotationNameParameter getAnnotateWithRoiIndex() {
        return annotateWithRoiIndex;
    }

    @JIPipeParameter("annotate-with-roi-index")
    public void setAnnotateWithRoiIndex(OptionalTextAnnotationNameParameter annotateWithRoiIndex) {
        this.annotateWithRoiIndex = annotateWithRoiIndex;
    }

    @SetJIPipeDocumentation(name = "Annotate with ROI name", description = "If enabled, annotate the output image with the mirror ROI name")
    @JIPipeParameter("annotate-with-roi-name")
    public OptionalTextAnnotationNameParameter getAnnotateWithRoiName() {
        return annotateWithRoiName;
    }

    @JIPipeParameter("annotate-with-roi-name")
    public void setAnnotateWithRoiName(OptionalTextAnnotationNameParameter annotateWithRoiName) {
        this.annotateWithRoiName = annotateWithRoiName;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus srcImage = iterationStep.getInputData("Input", ImagePlusData.class, progressInfo).getImage();
        ROI2DListData rois = iterationStep.getInputData("Mirror", ROI2DListData.class, progressInfo);

        for (int i = 0; i < rois.size(); i++) {
            Roi roi = rois.get(i);
            FloatPolygon polygon = roi.getFloatPolygon();
            if (polygon.npoints != 2) {
                ROI2DListData single = new ROI2DListData();
                single.add(roi);
                single.outline(RoiOutline.OrientedLine);
                polygon = single.get(0).getFloatPolygon();
            }
            if (polygon.npoints != 2) {
                throw new UnsupportedOperationException("Unable to convert ROI #" + i  + " to a line!");
            }

            int x1_ = (int) polygon.xpoints[0];
            int x2_ = (int) polygon.xpoints[1];
            int y1_ = (int) polygon.ypoints[0];
            int y2_ = (int) polygon.ypoints[1];

            ImagePlus result = ImageJUtils.generateForEachIndexedZCTSlice(srcImage, (srcIp, index) -> {
                ImageProcessor targetIp = srcIp.duplicate();
                LineMirror.mirrorImage(targetIp, x1_, y1_, x2_, y2_, mode);
                return targetIp;
            }, progressInfo);

            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }
}
