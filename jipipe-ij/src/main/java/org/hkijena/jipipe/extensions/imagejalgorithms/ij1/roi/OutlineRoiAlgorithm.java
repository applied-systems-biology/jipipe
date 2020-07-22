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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Outline ROI", description = "Converts the ROI into polygons, bounding rectangles, or convex hulls.")
@JIPipeOrganization(menuPath = "ROI", nodeTypeCategory = ProcessorNodeTypeCategory.class)
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class OutlineRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private RoiOutline outline = RoiOutline.ClosedPolygon;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public OutlineRoiAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public OutlineRoiAlgorithm(OutlineRoiAlgorithm other) {
        super(other);
        this.outline = other.outline;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData data = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();
        data.outline(outline);
        dataBatch.addOutputData(getFirstOutputSlot(), data);
    }


    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @JIPipeDocumentation(name = "Outline method", description = "The outlining method that should be applied.")
    @JIPipeParameter("outline")
    public RoiOutline getOutline() {
        return outline;
    }

    @JIPipeParameter("outline")
    public void setOutline(RoiOutline outline) {
        this.outline = outline;
    }
}
