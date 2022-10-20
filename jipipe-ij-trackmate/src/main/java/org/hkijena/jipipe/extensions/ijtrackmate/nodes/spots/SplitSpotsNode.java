/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.NamedTextAnnotationGeneratorExpression;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.SpotFeatureVariableSource;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Split spots", description = "Creates a list for each individual spot")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking\nSplit/Merge")
@JIPipeInputSlot(value = SpotsCollectionData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = SpotsCollectionData.class, slotName = "Output", autoCreate = true)
public class SplitSpotsNode extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customVariables;
    private NamedTextAnnotationGeneratorExpression.List annotationGenerator = new NamedTextAnnotationGeneratorExpression.List();

    public SplitSpotsNode(JIPipeNodeInfo info) {
        super(info);
        this.customVariables = new CustomExpressionVariablesParameter(this);
    }

    public SplitSpotsNode(SplitSpotsNode other) {
        super(other);
        this.customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
        this.annotationGenerator = new NamedTextAnnotationGeneratorExpression.List(other.annotationGenerator);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        SpotsCollectionData spotsCollectionData = dataBatch.getInputData(getFirstInputSlot(), SpotsCollectionData.class, progressInfo);
        SpotCollection oldCollection = spotsCollectionData.getSpots();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
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
            List<JIPipeTextAnnotation> annotations = new ArrayList<>();
            variables.set("name", spot.getName());
            variables.set("id", spot.ID());
            variables.set("index", index);
            for (Map.Entry<String, Double> entry : spot.getFeatures().entrySet()) {
                String variableName = SpotFeatureVariableSource.keyToVariable(entry.getKey());
                variables.set(variableName, entry.getValue());
            }
            for (NamedTextAnnotationGeneratorExpression expression : annotationGenerator) {
                annotations.add(expression.generateTextAnnotation(annotations, variables));
            }
            SpotsCollectionData newSpotsCollectionData = new SpotsCollectionData(spotsCollectionData);
            SpotCollection newCollection = new SpotCollection();
            newCollection.add(spot, spot.getFeature(Spot.FRAME).intValue());
            newSpotsCollectionData.getModel().setSpots(newCollection, true);
            dataBatch.addOutputData(getFirstOutputSlot(), newSpotsCollectionData, annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
            ++index;
        }
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "This list contains expressions to generate annotations for each spot")
    @JIPipeParameter("generated-annotations")
    @ExpressionParameterSettingsVariable(fromClass = SpotFeatureVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Spot ID", key = "id", description = "Numeric spot ID. Please note that the ID is not necessarily consecutive.")
    @ExpressionParameterSettingsVariable(name = "Spot index", key = "index", description = "Numeric index.")
    @ExpressionParameterSettingsVariable(name = "Number of spots", key = "n_spots", description = "The total number of spots")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public NamedTextAnnotationGeneratorExpression.List getAnnotationGenerator() {
        return annotationGenerator;
    }

    @JIPipeParameter("generated-annotations")
    public void setAnnotationGenerator(NamedTextAnnotationGeneratorExpression.List annotationGenerator) {
        this.annotationGenerator = annotationGenerator;
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }
}
