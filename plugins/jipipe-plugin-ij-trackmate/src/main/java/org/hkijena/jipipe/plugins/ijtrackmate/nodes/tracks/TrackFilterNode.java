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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes.tracks;

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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.TrackFeatureVariablesInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Filter tracks", description = "Filter TrackMate spots via expressions")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nFilter")
@AddJIPipeInputSlot(value = TrackCollectionData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = TrackCollectionData.class, name = "Output", create = true)
public class TrackFilterNode extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("track_displacement > 10");

    public TrackFilterNode(JIPipeNodeInfo info) {
        super(info);
    }

    public TrackFilterNode(TrackFilterNode other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        TrackCollectionData trackCollectionData = (TrackCollectionData) new TrackCollectionData(iterationStep.getInputData(getFirstInputSlot(), TrackCollectionData.class, progressInfo)).duplicate(progressInfo);

        trackCollectionData.computeTrackFeatures(progressInfo.resolve("Compute features"));

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        // Define all.* variables
        Map<String, List<Object>> allVariables = new HashMap<>();
        for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
            String variableName = TrackFeatureVariablesInfo.keyToVariable(trackFeature);
            allVariables.put(variableName, new ArrayList<>());
        }
        for (Integer trackID : trackCollectionData.getTrackModel().trackIDs(true)) {
            for (String trackFeature : trackCollectionData.getModel().getFeatureModel().getTrackFeatures()) {
                Double feature = trackCollectionData.getModel().getFeatureModel().getTrackFeature(trackID, trackFeature);
                if (feature == null)
                    feature = Double.NaN;

                String variableName = TrackFeatureVariablesInfo.keyToVariable(trackFeature);
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

                String variableName = TrackFeatureVariablesInfo.keyToVariable(trackFeature);
                variables.set(variableName, feature);
            }
            if (!filter.test(variables)) {
                trackCollectionData.getModel().setTrackVisibility(trackID, false);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), trackCollectionData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Keep track if", description = "The expression is executed per track. If it returns TRUE, the track is kept.")
    @JIPipeParameter(value = "filter", important = true)
    @JIPipeExpressionParameterSettings(hint = "per track")
    @AddJIPipeExpressionParameterVariable(fromClass = TrackFeatureVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
