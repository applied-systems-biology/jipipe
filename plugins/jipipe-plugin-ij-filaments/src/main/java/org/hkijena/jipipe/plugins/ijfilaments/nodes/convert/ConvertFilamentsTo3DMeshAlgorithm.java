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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.convert;

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
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalFloatParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.plugins.scene3d.datatypes.Scene3DData;

import java.awt.*;
import java.util.Set;

@SetJIPipeDocumentation(name = "Convert filaments to 3D scene", description = "Converts 3D filaments into a 3D scene.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", create = true)
public class ConvertFilamentsTo3DMeshAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String meshNamePrefix;
    private OptionalColorParameter overrideVertexColor = new OptionalColorParameter(Color.RED, false);
    private OptionalColorParameter overrideEdgeColor = new OptionalColorParameter(Color.RED, false);
    private Quantity.LengthUnit meshLengthUnit = Quantity.LengthUnit.mm;
    private boolean forceMeshLengthUnit = true;
    private boolean physicalSizes = true;
    private boolean withEdges = true;
    private boolean withVertices = true;
    private OptionalFloatParameter overrideEdgeRadius = new OptionalFloatParameter(false, 1);
    private OptionalFloatParameter overrideVertexRadius = new OptionalFloatParameter(false, 1);

    private boolean splitIntoConnectedComponents = true;

    public ConvertFilamentsTo3DMeshAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertFilamentsTo3DMeshAlgorithm(ConvertFilamentsTo3DMeshAlgorithm other) {
        super(other);
        this.meshNamePrefix = other.meshNamePrefix;
        this.meshLengthUnit = other.meshLengthUnit;
        this.forceMeshLengthUnit = other.forceMeshLengthUnit;
        this.physicalSizes = other.physicalSizes;
        this.overrideVertexColor = new OptionalColorParameter(other.overrideVertexColor);
        this.overrideEdgeColor = new OptionalColorParameter(other.overrideEdgeColor);
        this.withEdges = other.withEdges;
        this.withVertices = other.withVertices;
        this.overrideEdgeRadius = new OptionalFloatParameter(other.overrideEdgeRadius);
        this.overrideVertexRadius = new OptionalFloatParameter(other.overrideVertexRadius);
        this.splitIntoConnectedComponents = other.splitIntoConnectedComponents;
    }

    @SetJIPipeDocumentation(name = "Override vertex color", description = "Overrides the mesh color of vertices")
    @JIPipeParameter("override-vertex-color")
    public OptionalColorParameter getOverrideVertexColor() {
        return overrideVertexColor;
    }

    @JIPipeParameter("override-vertex-color")
    public void setOverrideVertexColor(OptionalColorParameter overrideVertexColor) {
        this.overrideVertexColor = overrideVertexColor;
    }

    @SetJIPipeDocumentation(name = "Override edge color", description = "Overrides the mesh color of edges")
    @JIPipeParameter("override-edge-color")
    public OptionalColorParameter getOverrideEdgeColor() {
        return overrideEdgeColor;
    }

    @JIPipeParameter("override-edge-color")
    public void setOverrideEdgeColor(OptionalColorParameter overrideEdgeColor) {
        this.overrideEdgeColor = overrideEdgeColor;
    }

    @SetJIPipeDocumentation(name = "With edges", description = "If enabled, convert edges into 3D meshes")
    @JIPipeParameter("with-edges")
    public boolean isWithEdges() {
        return withEdges;
    }

    @JIPipeParameter("with-edges")
    public void setWithEdges(boolean withEdges) {
        this.withEdges = withEdges;
    }

    @SetJIPipeDocumentation(name = "With vertices", description = "If enabled, convert vertices into 3D meshes")
    @JIPipeParameter("with-vertices")
    public boolean isWithVertices() {
        return withVertices;
    }

    @JIPipeParameter("with-vertices")
    public void setWithVertices(boolean withVertices) {
        this.withVertices = withVertices;
    }

    @SetJIPipeDocumentation(name = "Override edge radius", description = "If enabled, set the radius of edge meshes. Otherwise, the radius will interpolate between the radii of the two vertices")
    @JIPipeParameter("override-edge-radius")
    public OptionalFloatParameter getOverrideEdgeRadius() {
        return overrideEdgeRadius;
    }

    @JIPipeParameter("override-edge-radius")
    public void setOverrideEdgeRadius(OptionalFloatParameter overrideEdgeRadius) {
        this.overrideEdgeRadius = overrideEdgeRadius;
    }

    @SetJIPipeDocumentation(name = "Override vertex radius", description = "If enabled, set the radius of vertices.")
    @JIPipeParameter("override-vertex-radius")
    public OptionalFloatParameter getOverrideVertexRadius() {
        return overrideVertexRadius;
    }

    @JIPipeParameter("override-vertex-radius")
    public void setOverrideVertexRadius(OptionalFloatParameter overrideVertexRadius) {
        this.overrideVertexRadius = overrideVertexRadius;
    }

    @SetJIPipeDocumentation(name = "Mesh length", description = "If 'Force mesh length' and 'Physical dimensions' are enabled, scale the mesh to that 1 unit in its coordinate system is of the specified unit.")
    @JIPipeParameter("mesh-length-unit")
    public Quantity.LengthUnit getMeshLengthUnit() {
        return meshLengthUnit;
    }

    @JIPipeParameter("mesh-length-unit")
    public void setMeshLengthUnit(Quantity.LengthUnit meshLengthUnit) {
        this.meshLengthUnit = meshLengthUnit;
    }

    @SetJIPipeDocumentation(name = "Force mesh length", description = "If this option and 'Physical dimensions' are enabled, scale the mesh to that 1 unit in its coordinate system is of the unit specified in 'Mesh length'.")
    @JIPipeParameter("force-mesh-length-unit")
    public boolean isForceMeshLengthUnit() {
        return forceMeshLengthUnit;
    }

    @JIPipeParameter("force-mesh-length-unit")
    public void setForceMeshLengthUnit(boolean forceMeshLengthUnit) {
        this.forceMeshLengthUnit = forceMeshLengthUnit;
    }

    @SetJIPipeDocumentation(name = "Physical sizes", description = "If enabled, the physical voxel size is considered during the generation of the mesh")
    @JIPipeParameter("physical-sizes")
    public boolean isPhysicalSizes() {
        return physicalSizes;
    }

    @JIPipeParameter("physical-sizes")
    public void setPhysicalSizes(boolean physicalSizes) {
        this.physicalSizes = physicalSizes;
    }

    @SetJIPipeDocumentation(name = "Mesh name", description = "The prefix of the mesh")
    @JIPipeParameter("mesh-name-prefix")
    public String getMeshNamePrefix() {
        return meshNamePrefix;
    }

    @JIPipeParameter("mesh-name-prefix")
    public void setMeshNamePrefix(String meshNamePrefix) {
        this.meshNamePrefix = meshNamePrefix;
    }

    @SetJIPipeDocumentation(name = "Split into connected components", description = "If enabled, one mesh group is created per connected component")
    @JIPipeParameter("split-into-connected-components")
    public boolean isSplitIntoConnectedComponents() {
        return splitIntoConnectedComponents;
    }

    @JIPipeParameter("split-into-connected-components")
    public void setSplitIntoConnectedComponents(boolean splitIntoConnectedComponents) {
        this.splitIntoConnectedComponents = splitIntoConnectedComponents;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Scene3DData scene3DData = new Scene3DData();
        if (splitIntoConnectedComponents) {
            for (Set<FilamentVertex> connectedSet : inputData.getConnectivityInspector().connectedSets()) {
                Filaments3DData component = inputData.extractShallowCopy(connectedSet);
                scene3DData.add(component.toScene3D(withVertices,
                        withEdges,
                        physicalSizes,
                        meshLengthUnit,
                        forceMeshLengthUnit,
                        overrideVertexRadius.getContentOrDefault(-1f),
                        overrideEdgeRadius.getContentOrDefault(-1f),
                        overrideVertexColor.getContentOrDefault(null),
                        overrideEdgeColor.getContentOrDefault(null),
                        meshNamePrefix));
            }
        } else {
            scene3DData.add(inputData.toScene3D(withVertices,
                    withEdges,
                    physicalSizes,
                    meshLengthUnit,
                    forceMeshLengthUnit,
                    overrideVertexRadius.getContentOrDefault(-1f),
                    overrideEdgeRadius.getContentOrDefault(-1f),
                    overrideVertexColor.getContentOrDefault(null),
                    overrideEdgeColor.getContentOrDefault(null),
                    meshNamePrefix));
        }
        iterationStep.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }
}
