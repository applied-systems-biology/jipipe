package org.hkijena.jipipe.extensions.ijfilaments.nodes.modify;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

@SetJIPipeDocumentation(name = "Set filament vertex physical sizes from image", description = "Sets the physical voxel sizes of all vertices to the data provided in the image. If the image has no calibration data, " +
        "the filament voxel sizes will be reset to 1 pixel.")
@DefineJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Filaments", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Intensity", description = "The calibration is extracted from this image", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class SetVertexPhysicalSizeFromImageAlgorithm extends JIPipeIteratingAlgorithm {
    public SetVertexPhysicalSizeFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SetVertexPhysicalSizeFromImageAlgorithm(SetVertexPhysicalSizeFromImageAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments = new Filaments3DData(iterationStep.getInputData("Filaments", Filaments3DData.class, progressInfo));
        ImagePlus img = iterationStep.getInputData("Intensity", ImagePlusGreyscaleData.class, progressInfo).getImage();
        Calibration calibration = img.getCalibration();
        for (FilamentVertex vertex : filaments.vertexSet()) {
            vertex.setPhysicalVoxelSizeX(new Quantity(1, Quantity.UNIT_PIXELS));
            vertex.setPhysicalVoxelSizeY(new Quantity(1, Quantity.UNIT_PIXELS));
            vertex.setPhysicalVoxelSizeZ(new Quantity(1, Quantity.UNIT_PIXELS));
            if (calibration != null) {
                if (!StringUtils.isNullOrEmpty(calibration.getXUnit())) {
                    vertex.setPhysicalVoxelSizeX(new Quantity(calibration.pixelWidth, calibration.getXUnit()));
                    vertex.setPhysicalVoxelSizeY(new Quantity(calibration.pixelWidth, calibration.getXUnit())); // X = Y condition
                }
                if (!StringUtils.isNullOrEmpty(calibration.getYUnit())) {
                    vertex.setPhysicalVoxelSizeY(new Quantity(calibration.pixelHeight, calibration.getYUnit()));
                }
                if (!StringUtils.isNullOrEmpty(calibration.getZUnit())) {
                    vertex.setPhysicalVoxelSizeZ(new Quantity(calibration.pixelDepth, calibration.getZUnit()));
                }
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }
}
