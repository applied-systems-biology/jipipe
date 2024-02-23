package org.hkijena.jipipe.extensions.scene3d.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DUnindexedMeshGeometry;
import org.hkijena.jipipe.extensions.scene3d.utils.MarchingCubes;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;

import java.awt.*;

@SetJIPipeDocumentation(name = "Mask to 3D scene", description = "Applies the 'Marching cubes' algorithm to convert a 3D mask into a 3D mesh.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", create = true)
public class MaskTo3DMeshAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private String meshName;
    private Color meshColor = Color.RED;
    private Quantity.LengthUnit meshLengthUnit = Quantity.LengthUnit.mm;
    private boolean forceMeshLengthUnit = true;

    private boolean physicalSizes = true;


    public MaskTo3DMeshAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MaskTo3DMeshAlgorithm(MaskTo3DMeshAlgorithm other) {
        super(other);
        this.meshName = other.meshName;
        this.meshColor = other.meshColor;
        this.meshLengthUnit = other.meshLengthUnit;
        this.forceMeshLengthUnit = other.forceMeshLengthUnit;
        this.physicalSizes = other.physicalSizes;
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

    @SetJIPipeDocumentation(name = "Mesh name", description = "The name of the generated mesh")
    @JIPipeParameter("mesh-name")
    public String getMeshName() {
        return meshName;
    }

    @JIPipeParameter("mesh-name")
    public void setMeshName(String meshName) {
        this.meshName = meshName;
    }

    @SetJIPipeDocumentation(name = "Mesh color", description = "The color (diffuse) of the mesh")
    @JIPipeParameter("mesh-color")
    public Color getMeshColor() {
        return meshColor;
    }

    @JIPipeParameter("mesh-color")
    public void setMeshColor(Color meshColor) {
        this.meshColor = meshColor;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = iterationStep.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleMaskData.class, progressInfo).getImage();
        progressInfo.log("Marching cubes ...");
        float[] vertices = MarchingCubes.marchingCubes(imp, 0, 0, 0, 0, physicalSizes, forceMeshLengthUnit, meshLengthUnit);
        progressInfo.log("Calculating normals ...");
        float[] normals = Scene3DUtils.generateUnindexedVertexNormalsFlat(vertices);
        boolean[] mask = Scene3DUtils.findUnindexedNaNNormalVertices(vertices, normals);
        vertices = Scene3DUtils.filterArray(vertices, mask, false);
        normals = Scene3DUtils.filterArray(normals, mask, false);
        Scene3DUnindexedMeshGeometry meshObject = new Scene3DUnindexedMeshGeometry(vertices, normals);
        meshObject.setColor(meshColor);
        Scene3DData scene3DData = new Scene3DData();
        scene3DData.add(meshObject);
        iterationStep.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }
}
