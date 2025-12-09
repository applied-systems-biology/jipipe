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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;

import java.awt.geom.Point2D;


@SetJIPipeDocumentation(name = "Outline 2D ROI (Centroid)", description = "Converts the ROI into point ROI that are the centroids of their inputs")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "Output", create = true)
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
        Roi2dListData inputRoi = iterationStep.getInputData(getFirstInputSlot(), Roi2dListData.class, progressInfo);
        Roi2dListData outputRoi = new Roi2dListData();
        for (Roi roi : inputRoi) {
            Point2D centroid = Roi2dListData.getCentroidDouble(roi);
            PointRoi roi1 = new PointRoi(centroid.getX(), centroid.getY());
            roi1.setPosition(roi.getCPosition(), roi.getZPosition(), roi.getTPosition());
            outputRoi.add(roi1);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputRoi, progressInfo);
    }
}
