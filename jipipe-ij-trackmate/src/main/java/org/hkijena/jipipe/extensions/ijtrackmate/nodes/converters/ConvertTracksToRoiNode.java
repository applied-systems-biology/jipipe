package org.hkijena.jipipe.extensions.ijtrackmate.nodes.converters;

import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.gui.EllipseRoi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackFeatureVariablesInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackSpotFeatureVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

@SetJIPipeDocumentation(name = "Convert tracks to ROI", description = "Converts TrackMate tracks into ROI lists. Each lists contains the spot ROI of one track.")
@ConfigureJIPipeNode(menuPath = "Tracking\nConvert", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class ConvertTracksToRoiNode extends JIPipeSimpleIteratingAlgorithm {

    private NamedTextAnnotationGeneratorExpression.List annotationGenerator = new NamedTextAnnotationGeneratorExpression.List();

    private OptionalJIPipeExpressionParameter roiName = new OptionalJIPipeExpressionParameter(false, "\"ID\" + id");
    private OptionalJIPipeExpressionParameter fillColor = new OptionalJIPipeExpressionParameter(false, "");
    private OptionalJIPipeExpressionParameter lineColor = new OptionalJIPipeExpressionParameter(false, "RGB_COLOR(255, 255, 0)");
    private OptionalJIPipeExpressionParameter lineWidth = new OptionalJIPipeExpressionParameter(false, "1");

    public ConvertTracksToRoiNode(JIPipeNodeInfo info) {
        super(info);
        this.annotationGenerator.add(new NamedTextAnnotationGeneratorExpression(new AnnotationGeneratorExpression("track.id"), "Track ID"));
    }

    public ConvertTracksToRoiNode(ConvertTracksToRoiNode other) {
        super(other);
        this.annotationGenerator = new NamedTextAnnotationGeneratorExpression.List(other.annotationGenerator);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = iterationStep.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo);
        ImagePlus image = trackCollectionData.getImage();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
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
                String variableName = TrackSpotFeatureVariablesInfo.trackKeyToVariable(trackFeature);
                variables.set(variableName, feature);
            }

            ROIListData rois = new ROIListData();
            int spotIndex = 0;
            for (Spot spot : spots) {
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

                String name = spot.getName();
                Color lineColor = Color.YELLOW;
                Color fillColor = null;
                int lineWidth = 1;

                if (this.roiName.isEnabled() || this.fillColor.isEnabled() || this.lineColor.isEnabled() || this.lineWidth.isEnabled()) {
                    // Generate variables
                    for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                        String variableName = TrackSpotFeatureVariablesInfo.spotKeyToVariable(entry.getKey());
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
                annotations.add(expression.generateTextAnnotation(annotations, variables));
            }
            iterationStep.addOutputData(getFirstOutputSlot(), rois, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            ++index;
        }
    }

    @SetJIPipeDocumentation(name = "Generated annotations", description = "This list contains expressions to generate annotations for each spot")
    @JIPipeParameter("generated-annotations")
    @JIPipeExpressionParameterVariable(fromClass = TrackFeatureVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Track ID", key = "track.id", description = "Numeric track ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of tracks", key = "n_tracks", description = "The total number of tracks")
    public NamedTextAnnotationGeneratorExpression.List getAnnotationGenerator() {
        return annotationGenerator;
    }

    @JIPipeParameter("generated-annotations")
    public void setAnnotationGenerator(NamedTextAnnotationGeneratorExpression.List annotationGenerator) {
        this.annotationGenerator = annotationGenerator;
    }

    @SetJIPipeDocumentation(name = "Fill color", description = "Allows to change the fill color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("fill-color")
    @JIPipeExpressionParameterVariable(fromClass = TrackSpotFeatureVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @JIPipeExpressionParameterVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    public void setFillColor(OptionalJIPipeExpressionParameter fillColor) {
        this.fillColor = fillColor;
    }

    @SetJIPipeDocumentation(name = "Line color", description = "Allows to change the line color when rendered as RGB and within ImageJ. " + ColorUtils.PARSE_COLOR_DESCRIPTION)
    @JIPipeParameter("line-color")
    @JIPipeExpressionParameterVariable(fromClass = TrackSpotFeatureVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @JIPipeExpressionParameterVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getLineColor() {
        return lineColor;
    }

    @JIPipeParameter("line-color")
    public void setLineColor(OptionalJIPipeExpressionParameter lineColor) {
        this.lineColor = lineColor;
    }

    @SetJIPipeDocumentation(name = "Line width", description = "Allows to change the line width when rendered as RGB and within ImageJ. The annotation value is converted to an integer.")
    @JIPipeParameter("line-width")
    @JIPipeExpressionParameterVariable(fromClass = TrackSpotFeatureVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @JIPipeExpressionParameterVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getLineWidth() {
        return lineWidth;
    }

    @JIPipeParameter("line-width")
    public void setLineWidth(OptionalJIPipeExpressionParameter lineWidth) {
        this.lineWidth = lineWidth;
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "Allows to change the ROI name")
    @JIPipeParameter("roi-name")
    @JIPipeExpressionParameterVariable(fromClass = TrackSpotFeatureVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Track ID", key = "track.id", description = "Numeric Track ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Track index", key = "track.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of tracks", key = "n_tracks", description = "The total Number of tracks")
    @JIPipeExpressionParameterVariable(name = "Spot ID", key = "spot.id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Spot index", key = "spot.index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    public OptionalJIPipeExpressionParameter getRoiName() {
        return roiName;
    }

    @JIPipeParameter("roi-name")
    public void setRoiName(OptionalJIPipeExpressionParameter roiName) {
        this.roiName = roiName;
    }
}
