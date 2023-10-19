package org.hkijena.jipipe.extensions.ijtrackmate.nodes.tracks;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackFeatureVariableSource;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Filter tracks", description = "Filter TrackMate spots via expressions")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nFilter")
@JIPipeInputSlot(value = TrackCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TrackCollectionData.class, slotName = "Output", autoCreate = true)
public class TrackFilterNode extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customVariables;
    private DefaultExpressionParameter filter = new DefaultExpressionParameter("track_displacement > 10");

    public TrackFilterNode(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    public TrackFilterNode(TrackFilterNode other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.filter = new DefaultExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = (TrackCollectionData) new TrackCollectionData(iterationStep.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo)).duplicate(progressInfo);

        trackCollectionData.computeTrackFeatures(progressInfo.resolve("Compute features"));

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customVariables.writeToVariables(variables, true, "custom.", true, "custom");

        // Define all.* variables
        Map<String, List<Object>> allVariables = new HashMap<>();
        for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
            String variableName = TrackFeatureVariableSource.keyToVariable(trackFeature);
            allVariables.put(variableName, new ArrayList<>());
        }
        for (Integer trackID : trackCollectionData.getTrackModel().trackIDs(true)) {
            for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
                Double feature = trackCollectionData.getModel().getFeatureModel().getTrackFeature(trackID, trackFeature);
                if (feature == null)
                    feature = Double.NaN;

                String variableName = TrackFeatureVariableSource.keyToVariable(trackFeature);
                allVariables.get(variableName).add(feature);
            }
        }
        for (Map.Entry<String, List<Object>> entry : allVariables.entrySet()) {
            variables.set("all." + entry.getKey(), entry.getValue());
        }

        // Go through all tracks
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
        iterationStep.addOutputData(getFirstOutputSlot(), trackCollectionData, progressInfo);
    }

    @JIPipeDocumentation(name = "Filter", description = "The expression is executed per track. If it returns TRUE, the track is kept.")
    @JIPipeParameter(value = "filter", important = true)
    @ExpressionParameterSettings(hint = "per track")
    @ExpressionParameterSettingsVariable(fromClass = TrackFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public DefaultExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(DefaultExpressionParameter filter) {
        this.filter = filter;
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }
}
