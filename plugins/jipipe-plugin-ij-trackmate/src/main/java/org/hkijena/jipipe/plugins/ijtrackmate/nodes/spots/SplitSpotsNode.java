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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.NamedTextAnnotationGeneratorExpression;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.plugins.ijtrackmate.utils.SpotFeatureVariablesInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Split spots", description = "Creates a list for each individual spot")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nSplit/Merge")
@AddJIPipeInputSlot(value = SpotsCollectionData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = SpotsCollectionData.class, name = "Output", create = true)
public class SplitSpotsNode extends JIPipeSimpleIteratingAlgorithm {
    private NamedTextAnnotationGeneratorExpression.List annotationGenerator = new NamedTextAnnotationGeneratorExpression.List();

    public SplitSpotsNode(JIPipeNodeInfo info) {
        super(info);
    }

    public SplitSpotsNode(SplitSpotsNode other) {
        super(other);
        this.annotationGenerator = new NamedTextAnnotationGeneratorExpression.List(other.annotationGenerator);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = iterationStep.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
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
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            variables.set("name", spot.getName());
            variables.set("id", spot.ID());
            variables.set("index", index);
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariablesInfo.keyToVariable(entry.getKey());
                variables.set(variableName, entry.getValue());
            }
            for (NamedTextAnnotationGeneratorExpression expression : annotationGenerator) {
                annotations.add(expression.generateTextAnnotation(annotations, variables));
            }
            SpotsCollectionData newSpotsCollectionData = new SpotsCollectionData(spotsCollectionData);
            SpotCollection newCollection = new SpotCollection();
            newCollection.add(spot, spot.getFeature(Spot.FRAME).intValue());
            newSpotsCollectionData.getModel().setSpots(newCollection, true);
            iterationStep.addOutputData(getFirstOutputSlot(), newSpotsCollectionData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            ++index;
        }
    }

    @SetJIPipeDocumentation(name = "Generated annotations", description = "This list contains expressions to generate annotations for each spot")
    @JIPipeParameter("generated-annotations")
    @AddJIPipeExpressionParameterVariable(fromClass = SpotFeatureVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @AddJIPipeExpressionParameterVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @AddJIPipeExpressionParameterVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public NamedTextAnnotationGeneratorExpression.List getAnnotationGenerator() {
        return annotationGenerator;
    }

    @JIPipeParameter("generated-annotations")
    public void setAnnotationGenerator(NamedTextAnnotationGeneratorExpression.List annotationGenerator) {
        this.annotationGenerator = annotationGenerator;
    }
}
