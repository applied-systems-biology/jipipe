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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.generate;

import ij.ImagePlus;
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Append rectangular ROI (referenced)", description = "Appends manually defines rectangular ROI to all input ROI lists. " +
        "Ths algorithm allows to add rectangles as margins to the reference image bounds.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
@Deprecated
@LabelAsJIPipeHidden
public class ReferencedAppendRectangularRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private Margin.List rectangles = new Margin.List();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public ReferencedAppendRectangularRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData currentData = (ROIListData) iterationStep.getInputData("ROI", ROIListData.class, progressInfo).duplicate(progressInfo);
        ImagePlus reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
        Rectangle bounds = new Rectangle(0, 0, reference.getWidth(), reference.getHeight());
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (Margin margin : rectangles) {
            currentData.add(new ShapeRoi(margin.getInsideArea(bounds, variables)));
        }
        iterationStep.addOutputData(getFirstOutputSlot(), currentData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Rectangles", description = "List of rectangles")
    @JIPipeParameter("rectangles")
    public Margin.List getRectangles() {
        return rectangles;
    }

    @JIPipeParameter("rectangles")
    public void setRectangles(Margin.List rectangles) {
        this.rectangles = rectangles;
    }
}
