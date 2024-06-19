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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.calibration;

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
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

@SetJIPipeDocumentation(name = "Copy physical dimensions", description = "Copies the phyiscal pixel sizes from the source to the target")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Source", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Target", create = true)
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
