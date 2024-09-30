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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.EdgeMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdgeVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;

import java.awt.*;
import java.util.Map;

@SetJIPipeDocumentation(name = "Set filament edge properties", description = "Allows to override various properties of the filament edges")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class ChangeFilamentEdgePropertiesManuallyAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final EdgeMaskParameter edgeMask;
    private OptionalColorParameter color = new OptionalColorParameter(new Color(0x3584E4), false);
    private StringAndStringPairParameter.List metadata = new StringAndStringPairParameter.List();

    public ChangeFilamentEdgePropertiesManuallyAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.edgeMask = new EdgeMaskParameter();
        registerSubParameter(edgeMask);
    }

    public ChangeFilamentEdgePropertiesManuallyAlgorithm(ChangeFilamentEdgePropertiesManuallyAlgorithm other) {
        super(other);
        this.color = other.color;
        this.edgeMask = new EdgeMaskParameter(other.edgeMask);
        this.metadata = new StringAndStringPairParameter.List(other.metadata);
        registerSubParameter(edgeMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

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
            if(color.isEnabled()) {
                edge.setColor(color.getContent());
            }

            // Metadata
            for (StringAndStringPairParameter entry : metadata) {
                edge.setMetadata(entry.getKey(), entry.getValue());
            }

        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Metadata", description = "Allows to set/overwrite metadata")
    @StringParameterSettings(monospace = true)
    @JIPipeParameter("metadata")
    public StringAndStringPairParameter.List getMetadata() {
        return metadata;
    }

    @JIPipeParameter("metadata")
    public void setMetadata(StringAndStringPairParameter.List metadata) {
        this.metadata = metadata;
    }

    @SetJIPipeDocumentation(name = "Color", description = "Allows to override the edge color")
    @JIPipeParameter("color")
    public OptionalColorParameter getColor() {
        return color;
    }

    @JIPipeParameter("color")
    public void setColor(OptionalColorParameter color) {
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
