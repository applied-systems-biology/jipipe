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

import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;

import java.awt.Rectangle;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Append rectangular ROI", description = "Appends manually defines rectangular ROI to all input ROI lists. " +
        "Ths algorithm allows to add rectangles as margins. As no reference image is available, the reference area is defined by the " +
        "bounds of the already existing ROI.")
@JIPipeOrganization(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class AppendRectangularRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Margin.List rectangles = new Margin.List();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public AppendRectangularRoiAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ROIListData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
        rectangles.addNewInstance();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public AppendRectangularRoiAlgorithm(AppendRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new Margin.List(other.rectangles);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData currentData = (ROIListData) dataBatch.getInputData(getFirstInputSlot(), ROIListData.class).duplicate();
        Rectangle bounds = currentData.getBounds();
        for (Margin margin : rectangles) {
            currentData.add(new ShapeRoi(margin.apply(bounds)));
        }
        dataBatch.addOutputData(getFirstOutputSlot(), currentData);
    }

    @JIPipeDocumentation(name = "Rectangles", description = "List of rectangles")
    @JIPipeParameter("rectangles")
    public Margin.List getRectangles() {
        return rectangles;
    }

    @JIPipeParameter("rectangles")
    public void setRectangles(Margin.List rectangles) {
        this.rectangles = rectangles;
    }
}
