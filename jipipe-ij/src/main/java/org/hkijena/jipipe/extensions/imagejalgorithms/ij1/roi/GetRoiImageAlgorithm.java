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
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Get ROI image", description = "Gets the associated image from a ROI. No output is generated if the ROI have no associated images.")
@JIPipeOrganization(menuPath = "ROI", algorithmCategory = JIPipeNodeCategory.Processor)
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Image", autoCreate = true)
public class GetRoiImageAlgorithm extends JIPipeIteratingAlgorithm {

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public GetRoiImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public GetRoiImageAlgorithm(GetRoiImageAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = dataBatch.getInputData("ROI", ROIListData.class);
        for (Map.Entry<Optional<ImagePlus>, ROIListData> entry : data.groupByReferenceImage().entrySet()) {
            if(entry.getKey().isPresent()) {
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(entry.getKey().get()).duplicate());
            }
        }
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }
}
