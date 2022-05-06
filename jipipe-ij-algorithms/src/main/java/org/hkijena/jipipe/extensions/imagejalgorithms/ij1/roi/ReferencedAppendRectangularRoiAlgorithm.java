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
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Append rectangular ROI (referenced)", description = "Appends manually defines rectangular ROI to all input ROI lists. " +
        "Ths algorithm allows to add rectangles as margins to the reference image bounds.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class ReferencedAppendRectangularRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private Margin.List rectangles = new Margin.List();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ReferencedAppendRectangularRoiAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("ROI", "The ROI", ROIListData.class)
                .addInputSlot("Reference", "Reference image used for generating the rectangle", ImagePlusData.class)
                .addOutputSlot("Output", "Output ROI", ROIListData.class, null)
                .seal()
                .build());
        rectangles.addNewInstance();
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public ReferencedAppendRectangularRoiAlgorithm(ReferencedAppendRectangularRoiAlgorithm other) {
        super(other);
        this.rectangles = new Margin.List(other.rectangles);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData currentData = (ROIListData) dataBatch.getInputData("ROI", ROIListData.class, progressInfo).duplicate(progressInfo);
        ImagePlus reference = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
        Rectangle bounds = new Rectangle(0, 0, reference.getWidth(), reference.getHeight());
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Margin margin : rectangles) {
            currentData.add(new ShapeRoi(margin.getInsideArea(bounds, variables)));
        }
        dataBatch.addOutputData(getFirstOutputSlot(), currentData, progressInfo);
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
