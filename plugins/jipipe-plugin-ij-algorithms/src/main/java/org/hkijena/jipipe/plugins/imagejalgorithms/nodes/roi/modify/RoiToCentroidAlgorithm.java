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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.modify;

import ij.gui.PointRoi;
import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;

import java.awt.geom.Point2D;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Convert to centroid", description = "Converts the ROI into point ROI that are the centroids of their inputs")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class RoiToCentroidAlgorithm extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RoiToCentroidAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RoiToCentroidAlgorithm(RoiToCentroidAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputRoi = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        ROIListData outputRoi = new ROIListData();
        for (Roi roi : inputRoi) {
            Point2D centroid = ROIListData.getCentroidDouble(roi);
            PointRoi roi1 = new PointRoi(centroid.getX(), centroid.getY());
            roi1.setPosition(roi.getCPosition(), roi.getZPosition(), roi.getTPosition());
            outputRoi.add(roi1);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputRoi, progressInfo);
    }
}
