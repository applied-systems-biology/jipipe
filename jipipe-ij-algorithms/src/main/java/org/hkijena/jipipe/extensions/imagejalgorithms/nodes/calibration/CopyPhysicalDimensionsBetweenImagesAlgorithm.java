package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.calibration;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Copy physical dimensions", description = "Copies the phyiscal pixel sizes from the source to the target")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Source", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Target", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
public class CopyPhysicalDimensionsBetweenImagesAlgorithm extends JIPipeIteratingAlgorithm {

    public CopyPhysicalDimensionsBetweenImagesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CopyPhysicalDimensionsBetweenImagesAlgorithm(CopyPhysicalDimensionsBetweenImagesAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus source = iterationStep.getInputData("Source", ImagePlusData.class, progressInfo).getImage();
        ImagePlus target = iterationStep.getInputData("Target", ImagePlusData.class, progressInfo).getDuplicateImage();

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

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(target), progressInfo);
    }
}
