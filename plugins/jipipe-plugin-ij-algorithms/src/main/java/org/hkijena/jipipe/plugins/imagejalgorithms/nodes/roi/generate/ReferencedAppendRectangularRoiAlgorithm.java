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

import ij.ImagePlus;
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;
import org.hkijena.jipipe.plugins.parameters.library.roi.MarginList;

import java.awt.*;


@SetJIPipeDocumentation(name = "Append rectangular ROI (referenced)", description = "Appends manually defines rectangular ROI to all input ROI lists. " +
        "Ths algorithm allows to add rectangles as margins to the reference image bounds.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Merge")
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "ROI", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true)
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "Output", create = true)
@Deprecated
@LabelAsJIPipeHidden
public class ReferencedAppendRectangularRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private MarginList rectangles = new MarginList();

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
        this.rectangles = new MarginList(other.rectangles);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Roi2dListData currentData = (Roi2dListData) iterationStep.getInputData("ROI", Roi2dListData.class, progressInfo).duplicate(progressInfo);
        ImagePlus reference = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getImage();
        Rectangle bounds = new Rectangle(0, 0, reference.getWidth(), reference.getHeight());
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        for (Margin margin : rectangles) {
            currentData.add(new ShapeRoi(margin.getInsideArea(bounds, variables)));
        }
        iterationStep.addOutputData(getFirstOutputSlot(), currentData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Rectangles", description = "List of rectangles")
    @JIPipeParameter("rectangles")
    public MarginList getRectangles() {
        return rectangles;
    }

    @JIPipeParameter("rectangles")
    public void setRectangles(MarginList rectangles) {
        this.rectangles = rectangles;
    }
}
