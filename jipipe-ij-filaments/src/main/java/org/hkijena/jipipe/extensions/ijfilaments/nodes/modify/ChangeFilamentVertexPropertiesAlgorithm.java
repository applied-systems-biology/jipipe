package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterSerializationMode;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertexVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.Map;

@JIPipeDocumentation(name = "Change filament vertex properties", description = "Allows to override various properties of the filament vertices")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class ChangeFilamentVertexPropertiesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter centroidX = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidY = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidZ = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidC = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidT = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter radius = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter value = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter physicalSizeX = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter physicalSizeY = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter physicalSizeZ = new JIPipeExpressionParameter("default");

    private final VertexMaskParameter vertexMask;

    public ChangeFilamentVertexPropertiesAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public ChangeFilamentVertexPropertiesAlgorithm(ChangeFilamentVertexPropertiesAlgorithm other) {
        super(other);
        this.centroidX = new JIPipeExpressionParameter(other.centroidX);
        this.centroidY = new JIPipeExpressionParameter(other.centroidY);
        this.centroidZ = new JIPipeExpressionParameter(other.centroidZ);
        this.centroidC = new JIPipeExpressionParameter(other.centroidC);
        this.centroidT = new JIPipeExpressionParameter(other.centroidT);
        this.radius = new JIPipeExpressionParameter(other.radius);
        this.value = new JIPipeExpressionParameter(other.value);
        this.physicalSizeX = new JIPipeExpressionParameter(other.physicalSizeX);
        this.physicalSizeY = new JIPipeExpressionParameter(other.physicalSizeY);
        this.physicalSizeZ = new JIPipeExpressionParameter(other.physicalSizeZ);
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        for (FilamentVertex vertex : vertexMask.filter(outputData, outputData.vertexSet(), variables)) {
            // Write variables
            for (Map.Entry<String, String> entry : vertex.getMetadata().entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }
            FilamentVertexVariablesInfo.writeToVariables(outputData, vertex, variables, "");

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

            // Radius
            variables.set("default", vertex.getRadius());
            vertex.setRadius(radius.evaluateToDouble(variables));

            // Intensity
            variables.set("default", vertex.getValue());
            vertex.setRadius(value.evaluateToDouble(variables));

            // Physical size X
            variables.set("default", vertex.getPhysicalVoxelSizeX().toString());
            vertex.setPhysicalVoxelSizeX(Quantity.parse(physicalSizeX.evaluateToString(variables)));

            // Physical size Y
            variables.set("default", vertex.getPhysicalVoxelSizeY().toString());
            vertex.setPhysicalVoxelSizeY(Quantity.parse(physicalSizeY.evaluateToString(variables)));

            // Physical size Z
            variables.set("default", vertex.getPhysicalVoxelSizeZ().toString());
            vertex.setPhysicalVoxelSizeZ(Quantity.parse(physicalSizeZ.evaluateToString(variables)));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Centroid X", description = "The X location of the centroid")
    @JIPipeParameter("centroid-x")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidX() {
        return centroidX;
    }

    @JIPipeParameter("centroid-x")
    public void setCentroidX(JIPipeExpressionParameter centroidX) {
        this.centroidX = centroidX;
    }


    @JIPipeDocumentation(name = "Centroid Y", description = "The Y location of the centroid")
    @JIPipeParameter("centroid-y")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidY() {
        return centroidY;
    }

    @JIPipeParameter("centroid-y")
    public void setCentroidY(JIPipeExpressionParameter centroidY) {
        this.centroidY = centroidY;
    }

    @JIPipeDocumentation(name = "Centroid Z", description = "The Z location of the centroid")
    @JIPipeParameter("centroid-z")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidZ() {
        return centroidZ;
    }

    @JIPipeParameter("centroid-z")
    public void setCentroidZ(JIPipeExpressionParameter centroidZ) {
        this.centroidZ = centroidZ;
    }

    @JIPipeDocumentation(name = "Centroid channel", description = "The channel/c location of the centroid")
    @JIPipeParameter("centroid-c")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidC() {
        return centroidC;
    }

    @JIPipeParameter("centroid-c")
    public void setCentroidC(JIPipeExpressionParameter centroidC) {
        this.centroidC = centroidC;
    }

    @JIPipeDocumentation(name = "Centroid frame", description = "The frame/t location of the centroid")
    @JIPipeParameter("centroid-t")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidT() {
        return centroidT;
    }

    @JIPipeParameter("centroid-t")
    public void setCentroidT(JIPipeExpressionParameter centroidT) {
        this.centroidT = centroidT;
    }

    @JIPipeDocumentation(name = "Radius", description = "The radius of the vertex")
    @JIPipeParameter("radius")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(JIPipeExpressionParameter radius) {
        this.radius = radius;
    }

    @JIPipeDocumentation(name = "Value", description = "The value of the vertex")
    @JIPipeParameter("value")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(JIPipeExpressionParameter value) {
        this.value = value;
    }

    @JIPipeDocumentation(name = "Physical voxel size X", description = "The physical size of a voxel (X). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-x")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getPhysicalSizeX() {
        return physicalSizeX;
    }

    @JIPipeParameter("physical-size-x")
    public void setPhysicalSizeX(JIPipeExpressionParameter physicalSizeX) {
        this.physicalSizeX = physicalSizeX;
    }

    @JIPipeDocumentation(name = "Physical voxel size Y", description = "The physical size of a voxel (Y). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-y")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getPhysicalSizeY() {
        return physicalSizeY;
    }

    @JIPipeParameter("physical-size-y")
    public void setPhysicalSizeY(JIPipeExpressionParameter physicalSizeY) {
        this.physicalSizeY = physicalSizeY;
    }

    @JIPipeDocumentation(name = "Physical voxel size Z", description = "The physical size of a voxel (Y). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-z")
    @JIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getPhysicalSizeZ() {
        return physicalSizeZ;
    }

    @JIPipeParameter("physical-size-z")
    public void setPhysicalSizeZ(JIPipeExpressionParameter physicalSizeZ) {
        this.physicalSizeZ = physicalSizeZ;
    }

    @JIPipeDocumentation(name = "Vertex mask", description = "Allows to only target a specific set of vertices.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
