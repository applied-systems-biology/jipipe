package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotFeatureVariablesInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Filter spots", description = "Filter TrackMate spots via expressions")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nFilter")
@AddJIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = SpotsCollectionData.class, slotName = "Output", create = true)
public class SpotFilterNode extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("quality > 30");

    public SpotFilterNode(JIPipeNodeInfo info) {
        super(info);
    }

    public SpotFilterNode(SpotFilterNode other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = new SpotsCollectionData(iterationStep.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo));
        SpotCollection newCollection = new SpotCollection();
        SpotCollection oldCollection = spotsCollectionData.getSpots();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);
        variables.set("n_spots", oldCollection.getNSpots(true));
        int index = 0;

        // Define all.* variables
        Map<String, List<Object>> allVariables = new HashMap<>();
        for (Spot spot : oldCollection.iterable(true)) {
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariablesInfo.keyToVariable(entry.getKey());
                allVariables.put(variableName, new ArrayList<>());
            }
        }
        for (Spot spot : oldCollection.iterable(true)) {
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariablesInfo.keyToVariable(entry.getKey());
                allVariables.get(variableName).add(entry.getValue());
            }
        }
        for (Map.Entry<String, List<Object>> entry : allVariables.entrySet()) {
            variables.set("all." + entry.getKey(), entry.getValue());
        }

        // Go through all spots
        for (Spot spot : oldCollection.iterable(true)) {
            variables.set("name", spot.getName());
            variables.set("id", spot.ID());
            variables.set("index", index);
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariablesInfo.keyToVariable(entry.getKey());
                variables.set(variableName, entry.getValue());
            }
            if (filter.test(variables)) {
                newCollection.add(spot, spot.getFeature(Spot.FRAME).intValue());
            }
        }
        spotsCollectionData.getModel().setSpots(newCollection, true);
        iterationStep.addOutputData(getFirstOutputSlot(), spotsCollectionData, progressInfo);
        ++index;
    }

    @SetJIPipeDocumentation(name = "Filter", description = "The expression is executed per spot. If it returns TRUE, the spot is kept.")
    @JIPipeParameter(value = "filter", important = true)
    @JIPipeExpressionParameterSettings(hint = "per spot")
    @JIPipeExpressionParameterVariable(fromClass = SpotFeatureVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @JIPipeExpressionParameterVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @JIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
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
