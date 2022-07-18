package org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters;

import fiji.plugin.trackmate.Spot;
import ij.gui.EllipseRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.OptionalDefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.variables.AnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotFeatureVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.util.Map;
import java.util.Optional;

@JIPipeDocumentation(name = "Convert spots to ROI", description = "Converts TrackMate spots into ROI")
@JIPipeNode(menuPath = "Tracking\nConvert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ConvertSpotsToRoiNode extends JIPipeSimpleIteratingAlgorithm {

    private OptionalDefaultExpressionParameter roiName = new OptionalDefaultExpressionParameter(false, "\"ID\" + id");
    private OptionalDefaultExpressionParameter fillColor = new OptionalDefaultExpressionParameter(false, "");
    private OptionalDefaultExpressionParameter lineColor = new OptionalDefaultExpressionParameter(false, "RGB_COLOR(255, 255, 0)");
    private OptionalDefaultExpressionParameter lineWidth = new OptionalDefaultExpressionParameter(false, "1");

    public ConvertSpotsToRoiNode(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertSpotsToRoiNode(ConvertSpotsToRoiNode other) {
        super(other);
        this.fillColor = new OptionalDefaultExpressionParameter(other.fillColor);
        this.lineColor = new OptionalDefaultExpressionParameter(other.lineColor);
        this.lineWidth = new OptionalDefaultExpressionParameter(other.lineWidth);
        this.roiName = new OptionalDefaultExpressionParameter(other.roiName);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData data = dataBatch.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        variables.set("n_spots", data.getSpots().getNSpots(true));
        ROIListData rois = spotsToROIList(data, variables);
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    public ROIListData spotsToROIList(SpotsCollectionData spotsCollectionData, ExpressionVariables variables) {
        ROIListData result = new ROIListData();
        int index = 0;
        for (Spot spot : spotsCollectionData.getSpots().iterable(true)) {
            double x = spot.getDoublePosition(0);
            double y = spot.getDoublePosition(1);
            int z = (int) spot.getFloatPosition(2);
            int t = Optional.ofNullable(spot.getFeature(Spot.POSITION_T)).orElse(-1d).intValue();
            double radius = Optional.ofNullable(spot.getFeature(Spot.RADIUS)).orElse(1d);

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
                    String variableName = SpotFeatureVariableSource.keyToVariable(entry.getKey());
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

    @JIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    @ExpressionParameterSettingsVariable(fromClass = SpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalDefaultExpressionParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalDefaultExpressionParameter fillColor) {
        this.fillColor = fillColor;
    }

    @JIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("line-color")
    @ExpressionParameterSettingsVariable(fromClass = SpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalDefaultExpressionParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalDefaultExpressionParameter lineColor) {
        this.lineColor = lineColor;
    }

    @JIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ. The annotation value is converted to an integer.")
    @JIPipeParameter("line-width")
    @ExpressionParameterSettingsVariable(fromClass = SpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalDefaultExpressionParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalDefaultExpressionParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @JIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @ExpressionParameterSettingsVariable(fromClass = SpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalDefaultExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalDefaultExpressionParameter roiName) {
        this.roiName = roiName;
    }
}
