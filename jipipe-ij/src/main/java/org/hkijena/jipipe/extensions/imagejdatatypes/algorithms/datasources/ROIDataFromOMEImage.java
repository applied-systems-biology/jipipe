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

package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

/**
 * Loads ROI data from a file via IJ.openFile()
 */
@JIPipeDocumentation(name = "Extract ROI from OME image", description = "Loads a ROI list from an OME image.")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Image", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ROIDataFromOMEImage extends JIPipeSimpleIteratingAlgorithm {

    /**
     * @param info the algorithm info
     */
    public ROIDataFromOMEImage(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public ROIDataFromOMEImage(ROIDataFromOMEImage other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEImageData omeImageData = dataBatch.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        if (omeImageData.getRois() != null)
            dataBatch.addOutputData(getFirstOutputSlot(), omeImageData.getRois(), progressInfo);
        else
            dataBatch.addOutputData(getFirstOutputSlot(), new ROIListData(), progressInfo);
    }
}
