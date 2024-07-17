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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes.converters;

import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.OptionalJIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.SpotFeatureVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.util.Map;
import java.util.Optional;

@SetJIPipeDocumentation(name = "Convert spots to ROI", description = "Converts TrackMate spots into ROI")
@ConfigureJIPipeNode(menuPath = "Tracking\nConvert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = SpotsCollectionData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class ConvertSpotsToRoiNode extends JIPipeSimpleIteratingAlgorithm {

    private OptionalJIPipeExpressionParameter roiName = new OptionalJIPipeExpressionParameter(false, "\"ID\" + id");
    private OptionalJIPipeExpressionParameter fillColor = new OptionalJIPipeExpressionParameter(false, "");
    private OptionalJIPipeExpressionParameter lineColor = new OptionalJIPipeExpressionParameter(false, "RGB_COLOR(255, 255, 0)");
    private OptionalJIPipeExpressionParameter lineWidth = new OptionalJIPipeExpressionParameter(false, "1");

    public ConvertSpotsToRoiNode(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertSpotsToRoiNode(ConvertSpotsToRoiNode other) {
        super(other);
        this.fillColor = new OptionalJIPipeExpressionParameter(other.fillColor);
        this.lineColor = new OptionalJIPipeExpressionParameter(other.lineColor);
        this.lineWidth = new OptionalJIPipeExpressionParameter(other.lineWidth);
        this.roiName = new OptionalJIPipeExpressionParameter(other.roiName);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData data = iterationStep.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        variables.set("n_spots", data.getSpots().getNSpots(true));
        ROI2DListData rois = spotsToROIList(data, variables);
        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    public ROI2DListData spotsToROIList(SpotsCollectionData spotsCollectionData, JIPipeExpressionVariablesMap variables) {
        ROI2DListData result = new ROI2DListData();
        int index = 0;
        ImagePlus image = spotsCollectionData.getImage();
        for (Spot spot : spotsCollectionData.getSpots().iterable(true)) {
            double x = spot.getDoublePosition(0) / image.getCalibration().pixelWidth;
            double y = spot.getDoublePosition(1) / image.getCalibration().pixelHeight;
            int z = (int) spot.getFloatPosition(2);
            int t = Optional.ofNullable(spot.getFeature(Spot.POSITION_T)).orElse(-1d).intValue();
            double radius = Optional.ofNullable(spot.getFeature(Spot.RADIUS)).orElse(1d) / image.getCalibration().pixelWidth;

            double x1 = x - radius;
            double x2 = x + radius;
            double y1 = y - radius;
            double y2 = y + radius;
            EllipseRoi roi = new EllipseRoi(x1, y1, x2, y2, 1);
            roi.setPosition(0, z + 1, t + 1);

            // Default values
            String name = spot.getName();
            Color lineColor = Color.YELLOW;
            Color fillColor = null;
            int lineWidth = 1;

            if (this.roiName.isEnabled() || this.fillColor.isEnabled() || this.lineColor.isEnabled() || this.lineWidth.isEnabled()) {
                // Generate variables
                for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                    String variableName = SpotFeatureVariablesInfo.keyToVariable(entry.getKey());
                    variables.set(variableName, entry.getValue());
                }
                variables.set("id", spot.ID());
                variables.set("index", index);

                if (this.roiName.isEnabled()) {
                    name = this.roiName.getContent().evaluateToString(variables);
                }
                if (this.fillColor.isEnabled()) {
                    fillColor = this.fillColor.getContent().evaluateToColor(variables);
                }
                if (this.lineColor.isEnabled()) {
                    lineColor = this.lineColor.getContent().evaluateToColor(variables);
                }
                if (this.lineWidth.isEnabled()) {
                    lineWidth = this.lineWidth.getContent().evaluateToInteger(variables);
                }
            }

            // Write to ROI
            roi.setName(name);
            roi.setStrokeColor(lineColor);
            roi.setFillColor(fillColor);
            roi.setStrokeWidth(lineWidth);

            result.add(roi);
            ++index;
        }
        return result;
    }

    @SetJIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    @AddJIPipeExpressionParameterVariable(fromClass = SpotFeatureVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @AddJIPipeExpressionParameterVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @AddJIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalJIPipeExpressionParameter fillColor) {
        this.fillColor = fillColor;
    }

    @SetJIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("line-color")
    @AddJIPipeExpressionParameterVariable(fromClass = SpotFeatureVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @AddJIPipeExpressionParameterVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @AddJIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalJIPipeExpressionParameter lineColor) {
        this.lineColor = lineColor;
    }

    @SetJIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ. The annotation value is converted to an integer.")
    @JIPipeParameter("line-width")
    @AddJIPipeExpressionParameterVariable(fromClass = SpotFeatureVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @AddJIPipeExpressionParameterVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @AddJIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalJIPipeExpressionParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @AddJIPipeExpressionParameterVariable(fromClass = SpotFeatureVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @AddJIPipeExpressionParameterVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @AddJIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalJIPipeExpressionParameter roiName) {
        this.roiName = roiName;
    }
}
