package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertexVariableSource;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.Map;

@JIPipeDocumentation(name = "Change filament vertex properties", description = "Allows to override various properties of the filament vertices")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class ChangeFilamentVertexPropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter centroidX = new DefaultExpressionParameter("default");
    private DefaultExpressionParameter centroidY = new DefaultExpressionParameter("default");

    private DefaultExpressionParameter centroidZ = new DefaultExpressionParameter("default");

    private DefaultExpressionParameter centroidC = new DefaultExpressionParameter("default");

    private DefaultExpressionParameter centroidT = new DefaultExpressionParameter("default");

    private DefaultExpressionParameter thickness = new DefaultExpressionParameter("default");

    private final CustomExpressionVariablesParameter customExpressionVariables;
    public ChangeFilamentVertexPropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
    }

    public ChangeFilamentVertexPropertiesAlgorithm(ChangeFilamentVertexPropertiesAlgorithm other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.centroidX = new DefaultExpressionParameter(other.centroidX);
        this.centroidY = new DefaultExpressionParameter(other.centroidY);
        this.centroidZ = new DefaultExpressionParameter(other.centroidZ);
        this.centroidC = new DefaultExpressionParameter(other.centroidC);
        this.centroidT = new DefaultExpressionParameter(other.centroidT);
        this.thickness = new DefaultExpressionParameter(other.thickness);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = dataBatch.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variables, true, "custom", true, "custom");

        for (FilamentVertex vertex : outputData.vertexSet()) {
            // Write variables
            for (Map.Entry<String, String> entry : vertex.getMetadata().entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }
            FilamentVertexVariableSource.writeToVariables(outputData, vertex, variables, "");

            // Centroid X
            variables.set("default", vertex.getSpatialLocation().getX());
            vertex.getSpatialLocation().setX(centroidX.evaluateToInteger(variables));

            // Centroid Y
            variables.set("default", vertex.getSpatialLocation().getY());
            vertex.getSpatialLocation().setY(centroidY.evaluateToInteger(variables));

            // Centroid Z
            variables.set("default", vertex.getSpatialLocation().getZ());
            vertex.getSpatialLocation().setZ(centroidZ.evaluateToInteger(variables));

            // Centroid C
            variables.set("default", vertex.getNonSpatialLocation().getChannel());
            vertex.getNonSpatialLocation().setChannel(centroidC.evaluateToInteger(variables));

            // Centroid T
            variables.set("default", vertex.getNonSpatialLocation().getFrame());
            vertex.getNonSpatialLocation().setFrame(centroidT.evaluateToInteger(variables));

            // Thickness
            variables.set("default", vertex.getThickness());
            vertex.setThickness(thickness.evaluateToInteger(variables));
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Centroid X", description = "The X location of the centroid")
    @JIPipeParameter("centroid-x")
    @ExpressionParameterSettingsVariable(name = "Default value", key = "default", description = "The current value")
    @ExpressionParameterSettingsVariable(fromClass = FilamentVertexVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @ExpressionParameterSettings(hint = "per vertex")
    public DefaultExpressionParameter getCentroidX() {
        return centroidX;
    }

    @JIPipeParameter("centroid-x")
    public void setCentroidX(DefaultExpressionParameter centroidX) {
        this.centroidX = centroidX;
    }


    @JIPipeDocumentation(name = "Centroid Y", description = "The Y location of the centroid")
    @JIPipeParameter("centroid-y")
    @ExpressionParameterSettingsVariable(name = "Default value", key = "default", description = "The current value")
    @ExpressionParameterSettingsVariable(fromClass = FilamentVertexVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @ExpressionParameterSettings(hint = "per vertex")
    public DefaultExpressionParameter getCentroidY() {
        return centroidY;
    }

    @JIPipeParameter("centroid-y")
    public void setCentroidY(DefaultExpressionParameter centroidY) {
        this.centroidY = centroidY;
    }

    @JIPipeDocumentation(name = "Centroid Z", description = "The Z location of the centroid")
    @JIPipeParameter("centroid-z")
    @ExpressionParameterSettingsVariable(name = "Default value", key = "default", description = "The current value")
    @ExpressionParameterSettingsVariable(fromClass = FilamentVertexVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @ExpressionParameterSettings(hint = "per vertex")
    public DefaultExpressionParameter getCentroidZ() {
        return centroidZ;
    }

    @JIPipeParameter("centroid-z")
    public void setCentroidZ(DefaultExpressionParameter centroidZ) {
        this.centroidZ = centroidZ;
    }

    @JIPipeDocumentation(name = "Centroid channel", description = "The channel/c location of the centroid")
    @JIPipeParameter("centroid-c")
    @ExpressionParameterSettingsVariable(name = "Default value", key = "default", description = "The current value")
    @ExpressionParameterSettingsVariable(fromClass = FilamentVertexVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @ExpressionParameterSettings(hint = "per vertex")
    public DefaultExpressionParameter getCentroidC() {
        return centroidC;
    }

    @JIPipeParameter("centroid-c")
    public void setCentroidC(DefaultExpressionParameter centroidC) {
        this.centroidC = centroidC;
    }

    @JIPipeDocumentation(name = "Centroid frame", description = "The frame/t location of the centroid")
    @JIPipeParameter("centroid-t")
    @ExpressionParameterSettingsVariable(name = "Default value", key = "default", description = "The current value")
    @ExpressionParameterSettingsVariable(fromClass = FilamentVertexVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @ExpressionParameterSettings(hint = "per vertex")
    public DefaultExpressionParameter getCentroidT() {
        return centroidT;
    }
    @JIPipeParameter("centroid-t")
    public void setCentroidT(DefaultExpressionParameter centroidT) {
        this.centroidT = centroidT;
    }

    @JIPipeDocumentation(name = "Thickness", description = "The thickness of the vertex")
    @JIPipeParameter("thickness")
    @ExpressionParameterSettingsVariable(name = "Default value", key = "default", description = "The current value")
    @ExpressionParameterSettingsVariable(fromClass = FilamentVertexVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @ExpressionParameterSettings(hint = "per vertex")
    public DefaultExpressionParameter getThickness() {
        return thickness;
    }

    @JIPipeParameter("thickness")
    public void setThickness(DefaultExpressionParameter thickness) {
        this.thickness = thickness;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
