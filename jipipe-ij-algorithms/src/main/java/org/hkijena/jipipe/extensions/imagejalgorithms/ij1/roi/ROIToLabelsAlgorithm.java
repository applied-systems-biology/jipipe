package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "ROI to Labels", description = "Converts ROI and an optional reference image into a label image. If no reference image is provided, " +
        "the dimensions are estimated from the ROI. The background color (where no ROI is located) is zero.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", description = "Optional reference image used to calculate the size of the output", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Labels", description = "Output label image", autoCreate = true)
public class ROIToLabelsAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalDefaultExpressionParameter roiToLabelTransformation = new OptionalDefaultExpressionParameter(false, "index");
    private boolean drawOutline = false;
    private boolean fillOutline = true;

    public ROIToLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ROIToLabelsAlgorithm(ROIToLabelsAlgorithm other) {
        super(other);
        this.roiToLabelTransformation = new OptionalDefaultExpressionParameter(other.roiToLabelTransformation);
        this.drawOutline = other.drawOutline;
        this.fillOutline = other.fillOutline;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ExpressionVariables parameters = new ExpressionVariables();
        parameters.putAnnotations(dataBatch.getMergedTextAnnotations());

        ROIListData rois = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);

        // Create result image
        ImagePlus result;
        if(dataBatch.getInputRow("Reference") >= 0) {
            ImagePlus reference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
            result = IJ.createHyperStack("Labels",
                    reference.getWidth(),
                    reference.getHeight(),
                    reference.getNChannels(),
                    reference.getNSlices(),
                    reference.getNFrames(),
                    32);
        }
        else {
            Rectangle bounds = rois.getBounds();
            int sx = bounds.width + bounds.x;
            int sy = bounds.height + bounds.y;
            int sz = 1;
            int sc = 1;
            int st = 1;
            for (Roi roi : rois) {
                int z = roi.getZPosition();
                int c = roi.getCPosition();
                int t = roi.getTPosition();
                sz = Math.max(sz, z);
                sc = Math.max(sc, c);
                st = Math.max(st, t);
            }
            result = IJ.createHyperStack("Labels",
                    sx,
                    sy,
                    sc,
                    sz,
                    st,
                    32);
        }

        // Calculate labels
        int[] labelMap = new int[rois.size()];
        if(roiToLabelTransformation.isEnabled()) {
            for (int i = 0; i < rois.size(); i++) {
                Roi roi = rois.get(i);
                parameters.set("name", StringUtils.nullToEmpty(roi.getName()));
                parameters.set("x", roi.getBounds().x);
                parameters.set("y", roi.getBounds().y);
                parameters.set("width", roi.getBounds().width);
                parameters.set("height", roi.getBounds().height);
                parameters.set("index", i + 1);
                int label = (int)(roiToLabelTransformation.getContent().evaluateToNumber(parameters));
                labelMap[i] = label;
            }
        }
        else {
            for (int i = 0; i < rois.size(); i++) {
                labelMap[i] = i + 1;
            }
        }

        // Apply labels
        for (int z = 0; z < result.getNSlices(); z++) {
            for (int c = 0; c < result.getNChannels(); c++) {
                for (int t = 0; t < result.getNFrames(); t++) {
                    int stackIndex = result.getStackIndex(c + 1, z + 1, t + 1);
                    ImageProcessor processor = result.getStack().getProcessor(stackIndex);

                    for (int i = 0; i < rois.size(); i++) {
                        Roi roi = rois.get(i);
                        int rz = roi.getZPosition();
                        int rc = roi.getCPosition();
                        int rt = roi.getTPosition();
                        if (rz != 0 && rz != (z + 1))
                            continue;
                        if (rc != 0 && rc != (c + 1))
                            continue;
                        if (rt != 0 && rt != (t + 1))
                            continue;

                        processor.setColor(labelMap[i]);
                        if(fillOutline)
                            processor.fill(roi);
                        if(drawOutline)
                            roi.drawPixels(processor);
                    }
                }
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(result), progressInfo);
    }

    @JIPipeDocumentation(name = "ROI to label function", description = "Expression that converts a ROI into its numeric label index. Must return a number.")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    @JIPipeParameter("roi-to-label-transformation")
    public OptionalDefaultExpressionParameter getRoiToLabelTransformation() {
        return roiToLabelTransformation;
    }

    @JIPipeParameter("roi-to-label-transformation")
    public void setRoiToLabelTransformation(OptionalDefaultExpressionParameter roiToLabelTransformation) {
        this.roiToLabelTransformation = roiToLabelTransformation;
    }

    @JIPipeDocumentation(name = "Draw outline",description = "If enabled, the label value is drawn as outline")
    @JIPipeParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @JIPipeParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @JIPipeDocumentation(name = "Fill outline",description = "If enabled, the ROI is filled with the label value")
    @JIPipeParameter("fill-outline")
    public boolean isFillOutline() {
        return fillOutline;
    }

    @JIPipeParameter("fill-outline")
    public void setFillOutline(boolean fillOutline) {
        this.fillOutline = fillOutline;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Index", "Automatically assigned index of the roi (min 1)", "index"));
            VARIABLES.add(new ExpressionParameterVariable("Name", "Name of the ROI (can be empty)", "name"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box X", "Top-left X coordinate of the bounding box around the ROI", "x"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box Y", "Top-left Y coordinate of the bounding box around around the ROI", "y"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box width", "Width of the bounding box around around the ROI", "width"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box height", "Height of the bounding box around around the ROI", "height"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
