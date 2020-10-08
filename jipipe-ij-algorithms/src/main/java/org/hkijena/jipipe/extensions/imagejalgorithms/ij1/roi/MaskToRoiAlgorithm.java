/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

import java.util.function.Consumer;
import java.util.function.Supplier;

@JIPipeDocumentation(name = "Mask to ROI", description = "Converts pixel values equal or higher than the given threshold to a ROI. This will create a single ROI that contains holes. If a higher-dimensional image is provided, the operation is applied for each slice.")
@JIPipeOrganization(menuPath = "ROI", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class MaskToRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int threshold = 255;
    private boolean invertThreshold = false;

    public MaskToRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MaskToRoiAlgorithm(MaskToRoiAlgorithm other) {
        super(other);
        this.threshold = other.threshold;
        this.invertThreshold = other.invertThreshold;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusGreyscaleMaskData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleMaskData.class);
        ROIListData result = new ROIListData();
        ImageJUtils.forEachIndexedZCTSlice(inputData.getImage(), (ip, index) -> {
            int threshold = ip.isInvertedLut()?255:0;
            if (!invertThreshold)
                threshold = (threshold==255)?0:255;
            ip.setThreshold(threshold, threshold, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(new ImagePlus("slice", ip));
            roi.setPosition(index.getC() + 1, index.getZ() + 1, index.getT() + 1);
            result.add(roi);
        });
        dataBatch.addOutputData(getFirstOutputSlot(), result);
    }

    @JIPipeDocumentation(name = "Threshold", description = "Pixel values equal or higher to this value are added to the ROI.")
    @JIPipeParameter("threshold")
    public int getThreshold() {
        return threshold;
    }

    @JIPipeParameter("threshold")
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    @JIPipeDocumentation(name = "Invert threshold", description = "Changes the behavior that values less than the threshold value are selected as ROI.")
    @JIPipeParameter("invert-threshold")
    public boolean isInvertThreshold() {
        return invertThreshold;
    }

    @JIPipeParameter("invert-threshold")
    public void setInvertThreshold(boolean invertThreshold) {
        this.invertThreshold = invertThreshold;
    }
}
