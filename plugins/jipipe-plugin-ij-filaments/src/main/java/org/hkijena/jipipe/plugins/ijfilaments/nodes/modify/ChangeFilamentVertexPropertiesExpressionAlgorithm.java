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
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertexVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.ColorUtils;

import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Set filament vertex properties (Expression)", description = "Allows to override various properties of the filament vertices using expressions")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class ChangeFilamentVertexPropertiesExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private JIPipeExpressionParameter centroidX = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidY = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidZ = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidC = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter centroidT = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter radius = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter color = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter value = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter physicalSizeX = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter physicalSizeY = new JIPipeExpressionParameter("default");
    private JIPipeExpressionParameter physicalSizeZ = new JIPipeExpressionParameter("default");
    private ParameterCollectionList metadata = ParameterCollectionList.containingCollection(MetadataEntry.class);

    public ChangeFilamentVertexPropertiesExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public ChangeFilamentVertexPropertiesExpressionAlgorithm(ChangeFilamentVertexPropertiesExpressionAlgorithm other) {
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
        this.metadata = new ParameterCollectionList(other.metadata);
        this.color = new JIPipeExpressionParameter(other.color);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        List<MetadataEntry> metadataEntries = metadata.mapToCollection(MetadataEntry.class);

        for (FilamentVertex vertex : VertexMaskParameter.filter(vertexMask.getFilter(), outputData, outputData.vertexSet(), variables)) {
            // Write variables
            for (Map.Entry<String, String> entry : vertex.getMetadata().entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }
            FilamentVertexVariablesInfo.writeToVariables(outputData, vertex, variables, "");

            // Centroid X
            variables.set("default", vertex.getSpatialLocation().getX());
            vertex.getSpatialLocation().setX(centroidX.evaluateToDouble(variables));

            // Centroid Y
            variables.set("default", vertex.getSpatialLocation().getY());
            vertex.getSpatialLocation().setY(centroidY.evaluateToDouble(variables));

            // Centroid Z
            variables.set("default", vertex.getSpatialLocation().getZ());
            vertex.getSpatialLocation().setZ(centroidZ.evaluateToDouble(variables));

            // Centroid C
            variables.set("default", vertex.getNonSpatialLocation().getChannel());
            vertex.getNonSpatialLocation().setChannel(centroidC.evaluateToInteger(variables));

            // Centroid T
            variables.set("default", vertex.getNonSpatialLocation().getFrame());
            vertex.getNonSpatialLocation().setFrame(centroidT.evaluateToInteger(variables));

            // Radius
            variables.set("default", vertex.getRadius());
            vertex.setRadius(radius.evaluateToDouble(variables));

            // Color
            variables.set("default", ColorUtils.colorToHexString(vertex.getColor()));
            vertex.setColor(radius.evaluateToColor(variables));

            // Intensity
            variables.set("default", vertex.getValue());
            vertex.setValue(value.evaluateToDouble(variables));

            // Physical size X
            variables.set("default", vertex.getPhysicalVoxelSizeX().toString());
            vertex.setPhysicalVoxelSizeX(Quantity.parse(physicalSizeX.evaluateToString(variables)));

            // Physical size Y
            variables.set("default", vertex.getPhysicalVoxelSizeY().toString());
            vertex.setPhysicalVoxelSizeY(Quantity.parse(physicalSizeY.evaluateToString(variables)));

            // Physical size Z
            variables.set("default", vertex.getPhysicalVoxelSizeZ().toString());
            vertex.setPhysicalVoxelSizeZ(Quantity.parse(physicalSizeZ.evaluateToString(variables)));

            // Metadata
            for (MetadataEntry metadataEntry : metadataEntries) {
                vertex.setMetadata(metadataEntry.getKey(), metadataEntry.getValue().evaluateToString(variables));
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Metadata", description = "Allows to set/override vertex metadata values")
    @JIPipeParameter("metadata")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    public ParameterCollectionList getMetadata() {
        return metadata;
    }

    @JIPipeParameter("metadata")
    public void setMetadata(ParameterCollectionList metadata) {
        this.metadata = metadata;
    }

    @SetJIPipeDocumentation(name = "Centroid X", description = "The X location of the centroid")
    @JIPipeParameter("centroid-x")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidX() {
        return centroidX;
    }

    @JIPipeParameter("centroid-x")
    public void setCentroidX(JIPipeExpressionParameter centroidX) {
        this.centroidX = centroidX;
    }


    @SetJIPipeDocumentation(name = "Centroid Y", description = "The Y location of the centroid")
    @JIPipeParameter("centroid-y")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidY() {
        return centroidY;
    }

    @JIPipeParameter("centroid-y")
    public void setCentroidY(JIPipeExpressionParameter centroidY) {
        this.centroidY = centroidY;
    }

    @SetJIPipeDocumentation(name = "Centroid Z", description = "The Z location of the centroid")
    @JIPipeParameter("centroid-z")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidZ() {
        return centroidZ;
    }

    @JIPipeParameter("centroid-z")
    public void setCentroidZ(JIPipeExpressionParameter centroidZ) {
        this.centroidZ = centroidZ;
    }

    @SetJIPipeDocumentation(name = "Centroid channel", description = "The channel/c location of the centroid")
    @JIPipeParameter("centroid-c")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidC() {
        return centroidC;
    }

    @JIPipeParameter("centroid-c")
    public void setCentroidC(JIPipeExpressionParameter centroidC) {
        this.centroidC = centroidC;
    }

    @SetJIPipeDocumentation(name = "Centroid frame", description = "The frame/t location of the centroid")
    @JIPipeParameter("centroid-t")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getCentroidT() {
        return centroidT;
    }

    @JIPipeParameter("centroid-t")
    public void setCentroidT(JIPipeExpressionParameter centroidT) {
        this.centroidT = centroidT;
    }

    @SetJIPipeDocumentation(name = "Radius", description = "The radius of the vertex")
    @JIPipeParameter("radius")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(JIPipeExpressionParameter radius) {
        this.radius = radius;
    }

    @SetJIPipeDocumentation(name = "Color", description = "The color of the vertex")
    @JIPipeParameter("color")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getColor() {
        return color;
    }

    @JIPipeParameter("color")
    public void setColor(JIPipeExpressionParameter color) {
        this.color = color;
    }

    @SetJIPipeDocumentation(name = "Value", description = "The value of the vertex")
    @JIPipeParameter("value")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(JIPipeExpressionParameter value) {
        this.value = value;
    }

    @SetJIPipeDocumentation(name = "Physical voxel size X", description = "The physical size of a voxel (X). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-x")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getPhysicalSizeX() {
        return physicalSizeX;
    }

    @JIPipeParameter("physical-size-x")
    public void setPhysicalSizeX(JIPipeExpressionParameter physicalSizeX) {
        this.physicalSizeX = physicalSizeX;
    }

    @SetJIPipeDocumentation(name = "Physical voxel size Y", description = "The physical size of a voxel (Y). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-y")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getPhysicalSizeY() {
        return physicalSizeY;
    }

    @JIPipeParameter("physical-size-y")
    public void setPhysicalSizeY(JIPipeExpressionParameter physicalSizeY) {
        this.physicalSizeY = physicalSizeY;
    }

    @SetJIPipeDocumentation(name = "Physical voxel size Z", description = "The physical size of a voxel (Y). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-z")
    @AddJIPipeExpressionParameterVariable(name = "Default value", key = "default", description = "The current value")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getPhysicalSizeZ() {
        return physicalSizeZ;
    }

    @JIPipeParameter("physical-size-z")
    public void setPhysicalSizeZ(JIPipeExpressionParameter physicalSizeZ) {
        this.physicalSizeZ = physicalSizeZ;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Allows to only target a specific set of vertices.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    public static class MetadataEntry extends AbstractJIPipeParameterCollection {
        private String key;
        private JIPipeExpressionParameter value;

        @SetJIPipeDocumentation(name = "Key")
        @JIPipeParameter("key")
        public String getKey() {
            return key;
        }

        @JIPipeParameter("key")
        public void setKey(String key) {
            this.key = key;
        }

        @JIPipeParameter("value")
        @SetJIPipeDocumentation(name = "Value")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
        public JIPipeExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(JIPipeExpressionParameter value) {
            this.value = value;
        }
    }
}
