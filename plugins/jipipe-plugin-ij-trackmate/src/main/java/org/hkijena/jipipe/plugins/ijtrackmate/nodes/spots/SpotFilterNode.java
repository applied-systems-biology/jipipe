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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.SpotFeatureVariablesInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Filter spots", description = "Filter TrackMate spots via expressions")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nFilter")
@AddJIPipeInputSlot(value = SpotsCollectionData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = SpotsCollectionData.class, name = "Output", create = true)
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
