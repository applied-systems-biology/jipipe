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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.generate;

import ij.ImagePlus;
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHidden;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Define rectangular ROI (referenced)", description = "Manually defines a rectangular ROI. This algorithm requires a reference " +
        "image, but also allows more flexibility in defining the rectangles.")
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
@Deprecated
@JIPipeHidden
public class ReferencedDefineRectangularRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private Margin.List rectangles = new Margin.List();
    private boolean split = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ReferencedDefineRectangularRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        rectangles.addNewInstance();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ReferencedDefineRectangularRoiAlgorithm(ReferencedDefineRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new Margin.List(other.rectangles);
        this.split = other.split;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus reference = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        Rectangle boundaries = new Rectangle(0, 0, reference.getWidth(), reference.getHeight());

        ROIListData currentData = new ROIListData();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Margin margin : rectangles) {
            Rectangle rectangle = margin.getInsideArea(boundaries, variables);
            currentData.add(new ShapeRoi(rectangle));
            if (split) {
                getFirstOutputSlot().addData(currentData, progressInfo);
                currentData = new ROIListData();
            }
        }
        if (!currentData.isEmpty()) {
            getFirstOutputSlot().addData(currentData, progressInfo);
        }
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
}
