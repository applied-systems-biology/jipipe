package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.calibration;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "Copy physical dimensions", description = "Copies the phyiscal pixel sizes from the source to the target")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Source", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Target", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
public class CopyPhysicalDimensionsBetweenImagesAlgorithm extends JIPipeIteratingAlgorithm {

    public CopyPhysicalDimensionsBetweenImagesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CopyPhysicalDimensionsBetweenImagesAlgorithm(CopyPhysicalDimensionsBetweenImagesAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus source = dataBatch.getInputData("Source", ImagePlusData.class, progressInfo).getImage();
        ImagePlus target = dataBatch.getInputData("Target", ImagePlusData.class, progressInfo).getDuplicateImage();

        Calibration sourceCalibration = source.getCalibration();
        Calibration targetCalibration = target.getCalibration();

        targetCalibration.pixelWidth = sourceCalibration.pixelWidth;
        targetCalibration.pixelHeight = sourceCalibration.pixelHeight;
        targetCalibration.pixelDepth = sourceCalibration.pixelDepth;
        targetCalibration.fps = sourceCalibration.fps;
        targetCalibration.frameInterval = sourceCalibration.frameInterval;
        targetCalibration.setXUnit(sourceCalibration.getXUnit());
        targetCalibration.setYUnit(sourceCalibration.getYUnit());
        targetCalibration.setZUnit(sourceCalibration.getZUnit());
        targetCalibration.setTimeUnit(sourceCalibration.getTimeUnit());
        targetCalibration.setValueUnit(sourceCalibration.getValueUnit());

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(target), progressInfo);
    }
}
