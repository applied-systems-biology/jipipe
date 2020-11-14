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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.io;

import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

/**
 * Imports {@link ImagePlusData} from the GUI
 */
@JIPipeDocumentation(name = "ROI from ImageJ", description = "Imports ROI from the ImageJ ROI manager")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class ROIFromGUI extends JIPipeSimpleIteratingAlgorithm {

    public ROIFromGUI(JIPipeNodeInfo info) {
        super(info);
    }

    public ROIFromGUI(ROIFromGUI other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        ROIListData rois = new ROIListData(RoiManager.getRoiManager());
        dataBatch.addOutputData(getFirstOutputSlot(), rois.duplicate());
    }
}
