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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.roi.Margin;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm.ITERATING_ALGORITHM_DESCRIPTION;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Define rectangular ROI (referenced)", description = "Manually defines a rectangular ROI. This algorithm requires a reference " +
        "image, but also allows more flexibility in defining the rectangles." + "\n\n" + ITERATING_ALGORITHM_DESCRIPTION)
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.DataSource)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class ReferencedDefineRectangularRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private Margin.List rectangles = new Margin.List();
    private boolean split = false;
    private boolean close = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public ReferencedDefineRectangularRoiAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Reference", ImagePlusData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
        rectangles.addNewInstance();
    }

    /**
     * Instantiates a new algorithm.
     *
     * @param other the other
     */
    public ReferencedDefineRectangularRoiAlgorithm(ReferencedDefineRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new Margin.List(other.rectangles);
        this.close = other.close;
        this.split = other.split;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus reference = dataInterface.getInputData(getFirstInputSlot(), ImagePlusData.class).getImage();
        Rectangle boundaries = new Rectangle(0, 0, reference.getWidth(), reference.getHeight());

        ROIListData currentData = new ROIListData();
        for (Margin margin : rectangles) {
            Rectangle rectangle = margin.apply(boundaries);
            currentData.addRectangle(rectangle, close);
            if (split) {
                getFirstOutputSlot().addData(currentData);
                currentData = new ROIListData();
            }
        }
        if (!currentData.isEmpty()) {
            getFirstOutputSlot().addData(currentData);
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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

    @JIPipeDocumentation(name = "Split output", description = "If enabled, each rectangle is put into its own ROI list")
    @JIPipeParameter("split")
    public boolean isSplit() {
        return split;
    }

    @JIPipeParameter("split")
    public void setSplit(boolean split) {
        this.split = split;
    }

    @JIPipeDocumentation(name = "Close polygon", description = "If true, the polygon shape is closed")
    @JIPipeParameter("close")
    public boolean isClose() {
        return close;
    }

    @JIPipeParameter("close")
    public void setClose(boolean close) {
        this.close = close;
    }
}
