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
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.nodes.utils.FilamentEdgeMetadataEntry;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.EdgeMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdgeVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.utils.ColorUtils;

import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Set filament edge properties (Expression)", description = "Allows to override various properties of the filament edges using expressions")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class ChangeFilamentEdgePropertiesExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final EdgeMaskParameter edgeMask;
    private JIPipeExpressionParameter color = new JIPipeExpressionParameter("default");
    private ParameterCollectionList metadata = ParameterCollectionList.containingCollection(FilamentEdgeMetadataEntry.class);

    public ChangeFilamentEdgePropertiesExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.edgeMask = new EdgeMaskParameter();
        registerSubParameter(edgeMask);
    }

    public ChangeFilamentEdgePropertiesExpressionAlgorithm(ChangeFilamentEdgePropertiesExpressionAlgorithm other) {
        super(other);
        this.color = new JIPipeExpressionParameter(other.color);
        this.edgeMask = new EdgeMaskParameter(other.edgeMask);
        this.metadata = new ParameterCollectionList(other.metadata);
        registerSubParameter(edgeMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        List<FilamentEdgeMetadataEntry> metadataEntries = metadata.mapToCollection(FilamentEdgeMetadataEntry.class);

        for (FilamentEdge edge : edgeMask.filter(outputData, outputData.edgeSet(), variables)) {
            // Write variables
            for (Map.Entry<String, String> entry : edge.getMetadata().entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }
            FilamentEdgeVariablesInfo.writeToVariables(outputData, edge, variables, "");

            // Color
            variables.set("default", ColorUtils.colorToHexString(edge.getColor()));
            edge.setColor(color.evaluateToColor(variables));

            // Metadata
            for (FilamentEdgeMetadataEntry metadataEntry : metadataEntries) {
                edge.setMetadata(metadataEntry.getKey(), metadataEntry.getValue().evaluateToString(variables));
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Color", description = "Expression that determines the edge color")
    @JIPipeParameter("color")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value (hex color string)")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Edge metadata", description = "A map containing the edge metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per edge")
    public JIPipeExpressionParameter getColor() {
        return color;
    }

    @JIPipeParameter("color")
    public void setColor(JIPipeExpressionParameter color) {
        this.color = color;
    }

    @SetJIPipeDocumentation(name = "Metadata", description = "Allows to set/override vertex metadata values")
    @JIPipeParameter("metadata")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public ParameterCollectionList getMetadata() {
        return metadata;
    }

    @JIPipeParameter("metadata")
    public void setMetadata(ParameterCollectionList metadata) {
        this.metadata = metadata;
    }

    @SetJIPipeDocumentation(name = "Edge mask", description = "Allows to only target a specific set of edges.")
    @JIPipeParameter("edge-filter")
    public EdgeMaskParameter getEdgeMask() {
        return edgeMask;
    }

}
