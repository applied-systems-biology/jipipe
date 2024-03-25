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

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;

@SetJIPipeDocumentation(name = "Set filament vertex physical sizes from image", description = "Sets the physical voxel sizes of all vertices to the data provided in the image. If the image has no calibration data, " +
        "the filament voxel sizes will be reset to 1 pixel.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
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
