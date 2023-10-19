package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
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
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotFeatureVariableSource;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Filter spots", description = "Filter TrackMate spots via expressions")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nFilter")
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = SpotsCollectionData.class, slotName = "Output", autoCreate = true)
public class SpotFilterNode extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customVariables;
    private DefaultExpressionParameter filter = new DefaultExpressionParameter("quality > 30");

    public SpotFilterNode(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    public SpotFilterNode(SpotFilterNode other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.filter = new DefaultExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = new SpotsCollectionData(iterationStep.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo));
        SpotCollection newCollection = new SpotCollection();
        SpotCollection oldCollection = spotsCollectionData.getSpots();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customVariables.writeToVariables(variables, true, "custom.", true, "custom");
        variables.set("n_spots", oldCollection.getNSpots(true));
        int index = 0;

        // Define all.* variables
        Map<String, List<Object>> allVariables = new HashMap<>();
        for (Spot spot : oldCollection.iterable(true)) {
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariableSource.keyToVariable(entry.getKey());
                allVariables.put(variableName, new ArrayList<>());
            }
        }
        for (Spot spot : oldCollection.iterable(true)) {
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariableSource.keyToVariable(entry.getKey());
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
                String variableName = SpotFeatureVariableSource.keyToVariable(entry.getKey());
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

    @JIPipeDocumentation(name = "Filter", description = "The expression is executed per spot. If it returns TRUE, the spot is kept.")
    @JIPipeParameter(value = "filter", important = true)
    @ExpressionParameterSettings(hint = "per spot")
    @ExpressionParameterSettingsVariable(fromClass = SpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
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
