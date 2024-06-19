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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.convert;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "ROI to Labels (Expression)", description = "Converts ROI and an optional reference image into a label image. " +
        "Each ROI is assigned its own label. Alternatively, the label can be set generated from an expression. If no reference image is provided, " +
        "the dimensions are estimated from the ROI. The background color (where no ROI is located) is zero.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "Optional reference image used to calculate the size of the output", create = true, optional = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Labels", description = "Output label image", create = true)
public class ROIToLabelsExpressionAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalJIPipeExpressionParameter roiToLabelTransformation = new OptionalJIPipeExpressionParameter(false, "index");
    private boolean drawOutline = false;
    private boolean fillOutline = true;

    public ROIToLabelsExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ROIToLabelsExpressionAlgorithm(ROIToLabelsExpressionAlgorithm other) {
        super(other);
        this.roiToLabelTransformation = new OptionalJIPipeExpressionParameter(other.roiToLabelTransformation);
        this.drawOutline = other.drawOutline;
        this.fillOutline = other.fillOutline;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap();
        parameters.putAnnotations(iterationStep.getMergedTextAnnotations());

        ROIListData rois = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);

        // Create result image
        ImagePlus result;
        if (iterationStep.getInputRow("Reference") >= 0) {
            ImagePlus reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
            result = IJ.createHyperStack("Labels",
                    reference.getWidth(),
                    reference.getHeight(),
                    reference.getNChannels(),
                    reference.getNSlices(),
                    reference.getNFrames(),
                    32);
        } else {
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
        if (roiToLabelTransformation.isEnabled()) {
            for (int i = 0; i < rois.size(); i++) {
                Roi roi = rois.get(i);
                parameters.set("name", StringUtils.nullToEmpty(roi.getName()));
                parameters.set("x", roi.getBounds().x);
                parameters.set("y", roi.getBounds().y);
                parameters.set("width", roi.getBounds().width);
                parameters.set("height", roi.getBounds().height);
                parameters.set("index", i + 1);
                int label = (int) (roiToLabelTransformation.getContent().evaluateToNumber(parameters));
                labelMap[i] = label;
            }
        } else {
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
                        if (fillOutline)
                            processor.fill(roi);
                        if (drawOutline)
                            roi.drawPixels(processor);
                    }
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(result), progressInfo);
    }

    @SetJIPipeDocumentation(name = "ROI to label function", description = "Expression that converts a ROI into its numeric label index. Must return a number. If disabled, each ROI is assigned its own label.")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    @JIPipeParameter("roi-to-label-transformation")
    public OptionalJIPipeExpressionParameter getRoiToLabelTransformation() {
        return roiToLabelTransformation;
    }

    @JIPipeParameter("roi-to-label-transformation")
    public void setRoiToLabelTransformation(OptionalJIPipeExpressionParameter roiToLabelTransformation) {
        this.roiToLabelTransformation = roiToLabelTransformation;
    }

    @SetJIPipeDocumentation(name = "Draw outline", description = "If enabled, the label value is drawn as outline")
    @JIPipeParameter("draw-outline")
    public boolean isDrawOutline() {
        return drawOutline;
    }

    @JIPipeParameter("draw-outline")
    public void setDrawOutline(boolean drawOutline) {
        this.drawOutline = drawOutline;
    }

    @SetJIPipeDocumentation(name = "Fill outline", description = "If enabled, the ROI is filled with the label value")
    @JIPipeParameter("fill-outline")
    public boolean isFillOutline() {
        return fillOutline;
    }

    @JIPipeParameter("fill-outline")
    public void setFillOutline(boolean fillOutline) {
        this.fillOutline = fillOutline;
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("index", "Index", "Automatically assigned index of the roi (min 1)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("name", "Name", "Name of the ROI (can be empty)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "Bounding box X", "Top-left X coordinate of the bounding box around the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("y", "Bounding box Y", "Top-left Y coordinate of the bounding box around around the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Bounding box width", "Width of the bounding box around around the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Bounding box height", "Height of the bounding box around around the ROI"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
