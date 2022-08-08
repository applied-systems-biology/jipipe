package org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters;

import fiji.plugin.trackmate.Spot;
import ij.gui.EllipseRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackFeatureVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackSpotFeatureVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

@JIPipeDocumentation(name = "Convert tracks to ROI", description = "Converts TrackMate tracks into ROI lists. Each lists contains the spot ROI of one track.")
@JIPipeNode(menuPath = "Tracking\nConvert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class ConvertTracksToRoiNode extends JIPipeSimpleIteratingAlgorithm {

    private NamedTextAnnotationGeneratorExpression.List annotationGenerator = new NamedTextAnnotationGeneratorExpression.List();

    private OptionalDefaultExpressionParameter roiName = new OptionalDefaultExpressionParameter(false, "\"ID\" + id");
    private OptionalDefaultExpressionParameter fillColor = new OptionalDefaultExpressionParameter(false, "");
    private OptionalDefaultExpressionParameter lineColor = new OptionalDefaultExpressionParameter(false, "RGB_COLOR(255, 255, 0)");
    private OptionalDefaultExpressionParameter lineWidth = new OptionalDefaultExpressionParameter(false, "1");

    public ConvertTracksToRoiNode(JIPipeNodeInfo info) {
        super(info);
        annotationGenerator.add(new NamedTextAnnotationGeneratorExpression(new AnnotationGeneratorExpression("track.id"), "Track ID"));
    }

    public ConvertTracksToRoiNode(ConvertTracksToRoiNode other) {
        super(other);
        annotationGenerator = new NamedTextAnnotationGeneratorExpression.List(other.annotationGenerator);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = dataBatch.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        variables.set("n_tracks", trackCollectionData.getTrackModel().nTracks(true));

        int index = 0;
        for (Integer trackID : trackCollectionData.getTrackModel().trackIDs(true)) {
            Set<Spot> spots = trackCollectionData.getTrackModel().trackSpots(trackID);
            variables.set("track.id", trackID);
            variables.set("track.index", index);
            variables.set("n_spots", spots.size());
            for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
                Double feature = trackCollectionData.getModel().getFeatureModel().getTrackFeature(trackID, trackFeature);
                if (feature == null)
                    feature = Double.NaN;
                String variableName = TrackSpotFeatureVariableSource.trackKeyToVariable(trackFeature);
                variables.set(variableName, feature);
            }

            ROIListData rois = new ROIListData();
            int spotIndex = 0;
            for (Spot spot : spots) {
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

                String name = spot.getName();
                Color lineColor = Color.YELLOW;
                Color fillColor = null;
                int lineWidth = 1;

                if (this.roiName.isEnabled() || this.fillColor.isEnabled() || this.lineColor.isEnabled() || this.lineWidth.isEnabled()) {
                    // Generate variables
                    for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                        String variableName = TrackSpotFeatureVariableSource.spotKeyToVariable(entry.getKey());
                        variables.set(variableName, entry.getValue());
                    }
                    variables.set("spot.id", spot.ID());
                    variables.set("spot.index", spotIndex);

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

                rois.add(roi);
                ++spotIndex;
            }

            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            for (NamedTextAnnotationGeneratorExpression expression : annotationGenerator) {
                expression.generateTextAnnotation(annotations, variables);
            }
            dataBatch.addOutputData(getFirstOutputSlot(), rois, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            ++index;
        }
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "This list contains expressions to generate annotations for each spot")
    @JIPipeParameter("generated-annotations")
    @ExpressionParameterSettingsVariable(fromClass = TrackFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Track ID", key = "track.id", description = "Numeric track ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of tracks", key = "n_tracks", description = "The total number of tracks")
    public NamedTextAnnotationGeneratorExpression.List getAnnotationGenerator() {
        return annotationGenerator;
    }

    @JIPipeParameter("generated-annotations")
    public void setAnnotationGenerator(NamedTextAnnotationGeneratorExpression.List annotationGenerator) {
        this.annotationGenerator = annotationGenerator;
    }

    @JIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    @ExpressionParameterSettingsVariable(fromClass = TrackSpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
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
    @ExpressionParameterSettingsVariable(fromClass = TrackSpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
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
    @ExpressionParameterSettingsVariable(fromClass = TrackSpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
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
    @ExpressionParameterSettingsVariable(fromClass = TrackSpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalDefaultExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalDefaultExpressionParameter roiName) {
        this.roiName = roiName;
    }
}
