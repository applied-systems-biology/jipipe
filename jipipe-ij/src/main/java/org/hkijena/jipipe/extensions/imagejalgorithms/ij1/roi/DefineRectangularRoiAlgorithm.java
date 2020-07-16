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
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.roi.RectangleList;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Define rectangular ROI", description = "Manually defines rectangular ROI")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.DataSource)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class DefineRectangularRoiAlgorithm extends JIPipeAlgorithm {

    private RectangleList rectangles = new RectangleList();
    private boolean split = false;
    private boolean close = true;

    /**
     * Instantiates a new algorithm.
     *
     * @param info the info
     */
    public DefineRectangularRoiAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
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
    public DefineRectangularRoiAlgorithm(DefineRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new RectangleList(other.rectangles);
        this.close = other.close;
        this.split = other.split;
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ROIListData currentData = new ROIListData();
        for (Rectangle rectangle : rectangles) {
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
    public RectangleList getRectangles() {
        return rectangles;
    }

    @JIPipeParameter("rectangles")
    public void setRectangles(RectangleList rectangles) {
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
