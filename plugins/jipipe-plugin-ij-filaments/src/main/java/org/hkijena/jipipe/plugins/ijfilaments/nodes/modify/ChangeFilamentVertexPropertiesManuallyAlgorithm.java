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
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;

import java.awt.*;

@SetJIPipeDocumentation(name = "Set filament vertex properties", description = "Allows to override various properties of the filament vertices")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class ChangeFilamentVertexPropertiesManuallyAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private OptionalDoubleParameter centroidX = new OptionalDoubleParameter(0, false);
    private OptionalDoubleParameter centroidY = new OptionalDoubleParameter(0, false);
    private OptionalDoubleParameter centroidZ = new OptionalDoubleParameter(0, false);
    private OptionalIntegerParameter centroidC = new OptionalIntegerParameter(false, 0);
    private OptionalIntegerParameter centroidT = new OptionalIntegerParameter(false, 0);
    private OptionalDoubleParameter radius = new OptionalDoubleParameter(0, false);
    private OptionalColorParameter color = new OptionalColorParameter(new Color(0xE5A50A), false);
    private OptionalDoubleParameter value = new OptionalDoubleParameter(0, false);
    private OptionalQuantity physicalSizeX = new OptionalQuantity(new Quantity(1, "px"), false);
    private OptionalQuantity physicalSizeY = new OptionalQuantity(new Quantity(1, "px"), false);
    private OptionalQuantity physicalSizeZ = new OptionalQuantity(new Quantity(1, "px"), false);
    private StringAndStringPairParameter.List metadata = new StringAndStringPairParameter.List();

    public ChangeFilamentVertexPropertiesManuallyAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public ChangeFilamentVertexPropertiesManuallyAlgorithm(ChangeFilamentVertexPropertiesManuallyAlgorithm other) {
        super(other);
        this.color = new OptionalColorParameter(other.color);
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        this.centroidC = new OptionalIntegerParameter(other.centroidC);
        this.centroidT = new OptionalIntegerParameter(other.centroidT);
        this.centroidX = new OptionalDoubleParameter(other.centroidX);
        this.centroidY = new OptionalDoubleParameter(other.centroidY);
        this.centroidZ = new OptionalDoubleParameter(other.centroidZ);
        this.radius = new OptionalDoubleParameter(other.radius);
        this.value = new OptionalDoubleParameter(other.value);
        this.physicalSizeX = new OptionalQuantity(other.physicalSizeX);
        this.physicalSizeY = new OptionalQuantity(other.physicalSizeY);
        this.physicalSizeZ = new OptionalQuantity(other.physicalSizeZ);
        this.metadata = new StringAndStringPairParameter.List(other.metadata);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        for (FilamentVertex vertex : VertexMaskParameter.filter(vertexMask.getFilter(), outputData, outputData.vertexSet(), variables)) {
            // Centroid X
            if (centroidX.isEnabled()) {
                vertex.getSpatialLocation().setX(centroidX.getContent());
            }

            // Centroid Y
            if (centroidY.isEnabled()) {
                vertex.getSpatialLocation().setY(centroidY.getContent());
            }

            // Centroid Z
            if (centroidZ.isEnabled()) {
                vertex.getSpatialLocation().setZ(centroidZ.getContent());
            }

            // Centroid C
            if (centroidC.isEnabled()) {
                vertex.getNonSpatialLocation().setChannel(centroidC.getContent());
            }

            // Centroid T
            if (centroidT.isEnabled()) {
                vertex.getNonSpatialLocation().setFrame(centroidT.getContent());
            }

            // Radius
            if (radius.isEnabled()) {
                vertex.setRadius(radius.getContent());
            }

            // Color
            if (color.isEnabled()) {
                vertex.setColor(color.getContent());
            }

            // Intensity
            if (value.isEnabled()) {
                vertex.setValue(value.getContent());
            }

            // Physical size X
            if (physicalSizeX.isEnabled()) {
                vertex.setPhysicalVoxelSizeX(new Quantity(physicalSizeX.getContent()));
            }

            // Physical size Y
            if (physicalSizeY.isEnabled()) {
                vertex.setPhysicalVoxelSizeY(new Quantity(physicalSizeY.getContent()));
            }

            // Physical size Z
            if (physicalSizeZ.isEnabled()) {
                vertex.setPhysicalVoxelSizeZ(new Quantity(physicalSizeZ.getContent()));
            }

            // Metadata
            for (StringAndStringPairParameter entry : metadata) {
                vertex.setMetadata(entry.getKey(), entry.getValue());
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Centroid X", description = "The X location of the centroid")
    @JIPipeParameter("centroid-x")
    public OptionalDoubleParameter getCentroidX() {
        return centroidX;
    }

    @JIPipeParameter("centroid-x")
    public void setCentroidX(OptionalDoubleParameter centroidX) {
        this.centroidX = centroidX;
    }


    @SetJIPipeDocumentation(name = "Centroid Y", description = "The Y location of the centroid")
    @JIPipeParameter("centroid-y")
    public OptionalDoubleParameter getCentroidY() {
        return centroidY;
    }

    @JIPipeParameter("centroid-y")
    public void setCentroidY(OptionalDoubleParameter centroidY) {
        this.centroidY = centroidY;
    }

    @SetJIPipeDocumentation(name = "Centroid Z", description = "The Z location of the centroid")
    @JIPipeParameter("centroid-z")
    public OptionalDoubleParameter getCentroidZ() {
        return centroidZ;
    }

    @JIPipeParameter("centroid-z")
    public void setCentroidZ(OptionalDoubleParameter centroidZ) {
        this.centroidZ = centroidZ;
    }

    @SetJIPipeDocumentation(name = "Centroid channel", description = "The channel/c location of the centroid")
    @JIPipeParameter("centroid-c")
    public OptionalIntegerParameter getCentroidC() {
        return centroidC;
    }

    @JIPipeParameter("centroid-c")
    public void setCentroidC(OptionalIntegerParameter centroidC) {
        this.centroidC = centroidC;
    }

    @SetJIPipeDocumentation(name = "Centroid frame", description = "The frame/t location of the centroid")
    @JIPipeParameter("centroid-t")
    public OptionalIntegerParameter getCentroidT() {
        return centroidT;
    }

    @JIPipeParameter("centroid-t")
    public void setCentroidT(OptionalIntegerParameter centroidT) {
        this.centroidT = centroidT;
    }

    @SetJIPipeDocumentation(name = "Radius", description = "The radius of the vertex")
    @JIPipeParameter("radius")
    public OptionalDoubleParameter getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public void setRadius(OptionalDoubleParameter radius) {
        this.radius = radius;
    }

    @SetJIPipeDocumentation(name = "Color", description = "The color of the vertex")
    @JIPipeParameter("color")
    public OptionalColorParameter getColor() {
        return color;
    }

    @JIPipeParameter("color")
    public void setColor(OptionalColorParameter color) {
        this.color = color;
    }

    @SetJIPipeDocumentation(name = "Value", description = "The value of the vertex")
    @JIPipeParameter("value")
    public OptionalDoubleParameter getValue() {
        return value;
    }

    @JIPipeParameter("value")
    public void setValue(OptionalDoubleParameter value) {
        this.value = value;
    }

    @SetJIPipeDocumentation(name = "Physical voxel size X", description = "The physical size of a voxel (X). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-x")
    public OptionalQuantity getPhysicalSizeX() {
        return physicalSizeX;
    }

    @JIPipeParameter("physical-size-x")
    public void setPhysicalSizeX(OptionalQuantity physicalSizeX) {
        this.physicalSizeX = physicalSizeX;
    }

    @SetJIPipeDocumentation(name = "Physical voxel size Y", description = "The physical size of a voxel (Y). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-y")
    public OptionalQuantity getPhysicalSizeY() {
        return physicalSizeY;
    }

    @JIPipeParameter("physical-size-y")
    public void setPhysicalSizeY(OptionalQuantity physicalSizeY) {
        this.physicalSizeY = physicalSizeY;
    }

    @SetJIPipeDocumentation(name = "Physical voxel size Z", description = "The physical size of a voxel (Y). Must return a string in the format '[Value] [Unit]'")
    @JIPipeParameter("physical-size-z")
    public OptionalQuantity getPhysicalSizeZ() {
        return physicalSizeZ;
    }

    @JIPipeParameter("physical-size-z")
    public void setPhysicalSizeZ(OptionalQuantity physicalSizeZ) {
        this.physicalSizeZ = physicalSizeZ;
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

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Allows to only target a specific set of vertices.")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
