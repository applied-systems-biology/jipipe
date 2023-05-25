package org.hkijena.jipipe.extensions.scene3d.nodes;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleMaskData;
import org.hkijena.jipipe.extensions.scene3d.datatypes.Scene3DData;
import org.hkijena.jipipe.extensions.scene3d.model.Scene3DMeshObject;
import org.hkijena.jipipe.extensions.scene3d.utils.MarchingCubes;
import org.hkijena.jipipe.extensions.scene3d.utils.Scene3DUtils;

import java.util.ArrayList;

@JIPipeDocumentation(name = "Mask to 3D scene", description = "Applies the 'Marching cubes' algorithm to convert a 3D mask into a 3D mesh.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Binary")
@JIPipeInputSlot(value = ImagePlus3DGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Scene3DData.class, slotName = "Output", autoCreate = true)
public class MaskTo3DMeshAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public MaskTo3DMeshAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MaskTo3DMeshAlgorithm(MaskTo3DMeshAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DGreyscaleMaskData.class, progressInfo).getImage();
        progressInfo.log("Marching cubes ...");
        float[] vertices = MarchingCubes.marchingCubes(imp, 0, 0);
        progressInfo.log("Calculating normals ...");
        float[] normals = Scene3DUtils.generateVertexNormalsFlat(vertices);
        boolean[] mask = Scene3DUtils.findNaNNormalVertices(vertices, normals);
        vertices = Scene3DUtils.filterArray(vertices, mask, false);
        normals = Scene3DUtils.filterArray(normals, mask, false);
        Scene3DMeshObject meshObject = new Scene3DMeshObject(vertices, normals);
        Scene3DData scene3DData = new Scene3DData();
        scene3DData.add(meshObject);
        dataBatch.addOutputData(getFirstOutputSlot(), scene3DData, progressInfo);
    }
}
