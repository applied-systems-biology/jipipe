package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.NamedTextAnnotationGeneratorExpression;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackFeatureVariablesInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JIPipeDocumentation(name = "Split tracks", description = "Creates a list for each individual track")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nSplit/Merge")
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TrackCollectionData.class, slotName = "Output", autoCreate = true)
public class SplitTracksNode extends JIPipeSimpleIteratingAlgorithm {

    private NamedTextAnnotationGeneratorExpression.List annotationGenerator = new NamedTextAnnotationGeneratorExpression.List();

    public SplitTracksNode(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitTracksNode(SplitTracksNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData oldTrackCollectionData = new TrackCollectionData(iterationStep.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo));
        oldTrackCollectionData.computeTrackFeatures(progressInfo.resolve("Compute features"));

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        variables.set("n_tracks", oldTrackCollectionData.getTrackModel().nTracks(true));
        int index = 0;
        Set<Integer> trackIds = oldTrackCollectionData.getTrackModel().trackIDs(true);
        for (Integer trackID : trackIds) {
            variables.set("index", index);
            variables.set("id", trackID);
            for (String trackFeature : oldTrackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
                Double feature = oldTrackCollectionData.getModel().getFeatureModel().getTrackFeature(trackID, trackFeature);
                if (feature == null)
                    feature = Double.NaN;

                String variableName = TrackFeatureVariablesInfo.keyToVariable(trackFeature);
                variables.set(variableName, feature);
            }
            TrackCollectionData newTrackCollectionData = new TrackCollectionData(oldTrackCollectionData);
            for (Integer otherId : trackIds) {
                newTrackCollectionData.getModel().setTrackVisibility(otherId, otherId.intValue() == trackID.intValue());
            }
            ++index;
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            for (NamedTextAnnotationGeneratorExpression expression : annotationGenerator) {
                annotations.add(expression.generateTextAnnotation(annotations, variables));
            }
            iterationStep.addOutputData(getFirstOutputSlot(), newTrackCollectionData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }

    }

    @JIPipeDocumentation(name = "Generated annotations", description = "This list contains expressions to generate annotations for each spot")
    @JIPipeParameter("generated-annotations")
    @JIPipeExpressionParameterVariable(fromClass = TrackFeatureVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Track ID", key = "id", description = "Numeric track ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Track index", key = "index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of tracks", key = "n_tracks", description = "The total number of tracks")
    public NamedTextAnnotationGeneratorExpression.List getAnnotationGenerator() {
        return annotationGenerator;
    }

    @JIPipeParameter("generated-annotations")
    public void setAnnotationGenerator(NamedTextAnnotationGeneratorExpression.List annotationGenerator) {
        this.annotationGenerator = annotationGenerator;
    }

}
