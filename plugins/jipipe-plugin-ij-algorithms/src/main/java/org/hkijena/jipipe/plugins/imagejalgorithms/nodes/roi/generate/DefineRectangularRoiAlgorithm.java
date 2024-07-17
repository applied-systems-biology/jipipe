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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.generate;

import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.parameters.library.roi.RectangleList;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Define rectangular ROI", description = "Manually defines rectangular ROI")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output")
@Deprecated
@LabelAsJIPipeHidden
public class DefineRectangularRoiAlgorithm extends JIPipeAlgorithm {

    private RectangleList rectangles = new RectangleList();
    private boolean split = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public DefineRectangularRoiAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addOutputSlot("Output", "", ROI2DListData.class, null)
                .seal()
                .build());
        rectangles.addNewInstance();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public DefineRectangularRoiAlgorithm(DefineRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new RectangleList(other.rectangles);
        this.split = other.split;
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData currentData = new ROI2DListData();
        for (Rectangle rectangle : rectangles) {
            currentData.add(new ShapeRoi(rectangle));
            if (split) {
                getFirstOutputSlot().addData(currentData, progressInfo);
                currentData = new ROI2DListData();
            }
        }
        if (!currentData.isEmpty()) {
            getFirstOutputSlot().addData(currentData, progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Rectangles", description = "List of rectangles")
    @JIPipeParameter("rectangles")
    public RectangleList getRectangles() {
        return rectangles;
    }

    @JIPipeParameter("rectangles")
    public void setRectangles(RectangleList rectangles) {
        this.rectangles = rectangles;
    }

    @SetJIPipeDocumentation(name = "Split output", description = "If enabled, each rectangle is put into its own ROI list")
    @JIPipeParameter("split")
    public boolean isSplit() {
        return split;
    }

    @JIPipeParameter("split")
    public void setSplit(boolean split) {
        this.split = split;
    }
}
