package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.AnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackFeatureVariableSource;

@JIPipeDocumentation(name = "Filter tracks", description = "Filter TrackMate spots via expressions")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nFilter")
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TrackCollectionData.class, slotName = "Output", autoCreate = true)
public class TrackFilterNode extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filter = new DefaultExpressionParameter("track_displacement > 10");

    public TrackFilterNode(JIPipeNodeInfo info) {
        super(info);
    }

    public TrackFilterNode(TrackFilterNode other) {
        super(other);
        this.filter = new DefaultExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = (TrackCollectionData) new TrackCollectionData(dataBatch.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo)).duplicate(progressInfo);

        trackCollectionData.computeTrackFeatures(progressInfo.resolve("Compute features"));

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Integer trackID : trackCollectionData.getTrackModel().trackIDs(true)) {
            for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
                Double feature = trackCollectionData.getModel().getFeatureModel().getTrackFeature(trackID, trackFeature);
                if (feature == null)
                    feature = Double.NaN;

                String variableName = TrackFeatureVariableSource.keyToVariable(trackFeature);
                variables.set(variableName, feature);
            }
            if (!filter.test(variables)) {
                trackCollectionData.getModel().setTrackVisibility(trackID, false);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), trackCollectionData, progressInfo);
    }

    @JIPipeDocumentation(name = "Filter", description = "The expression is executed per track. If it returns TRUE, the track is kept.")
    @JIPipeParameter(value = "filter", important = true)
    @ExpressionParameterSettingsVariable(fromClass = TrackFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = AnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(DefaultExpressionParameter filter) {
        this.filter = filter;
    }
}
