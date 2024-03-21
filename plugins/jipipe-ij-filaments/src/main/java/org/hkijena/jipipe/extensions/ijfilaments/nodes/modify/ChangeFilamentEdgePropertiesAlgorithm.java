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

package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.parameters.EdgeMaskParameter;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdgeVariablesInfo;
import org.hkijena.jipipe.utils.ColorUtils;

import java.util.Map;

@SetJIPipeDocumentation(name = "Change filament edge properties", description = "Allows to override various properties of the filament edges")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class ChangeFilamentEdgePropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter color = new JIPipeExpressionParameter("default");
    private final EdgeMaskParameter edgeMask;

    public ChangeFilamentEdgePropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.edgeMask = new EdgeMaskParameter();
        registerSubParameter(edgeMask);
    }

    public ChangeFilamentEdgePropertiesAlgorithm(ChangeFilamentEdgePropertiesAlgorithm other) {
        super(other);
        this.color = new JIPipeExpressionParameter(other.color);
        this.edgeMask = new EdgeMaskParameter(other.edgeMask);
        registerSubParameter(edgeMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        for (FilamentEdge edge : edgeMask.filter(outputData, outputData.edgeSet(), variables)) {
            // Write variables
            for (Map.Entry<String, String> entry : edge.getMetadata().entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }
            FilamentEdgeVariablesInfo.writeToVariables(outputData, edge, variables, "");

            // Color
            variables.set("default", ColorUtils.colorToHexString(edge.getColor()));
            edge.setColor(color.evaluateToColor(variables));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Color", description = "Expression that determines the edge color")
    @JIPipeParameter("color")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value (hex color string)")
    @JIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Edge metadata", description = "A map containing the edge metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per edge")
    public JIPipeExpressionParameter getColor() {
        return color;
    }

    @JIPipeParameter("color")
    public void setColor(JIPipeExpressionParameter color) {
        this.color = color;
    }

    @SetJIPipeDocumentation(name = "Edge mask", description = "Allows to only target a specific set of edges.")
    @JIPipeParameter("edge-filter")
    public EdgeMaskParameter getEdgeMask() {
        return edgeMask;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
