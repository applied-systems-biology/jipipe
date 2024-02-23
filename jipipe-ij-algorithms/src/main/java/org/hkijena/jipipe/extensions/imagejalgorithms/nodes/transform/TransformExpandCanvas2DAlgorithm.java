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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.NumericFunctionExpression;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;

import java.awt.*;

@SetJIPipeDocumentation(name = "Expand canvas 2D", description = "Pads each image slice with a background color.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust", aliasName = "Canvas Size...")
public class TransformExpandCanvas2DAlgorithm extends JIPipeIteratingAlgorithm {

    private NumericFunctionExpression xAxis = new NumericFunctionExpression();
    private NumericFunctionExpression yAxis = new NumericFunctionExpression();
    private Color backgroundColor = Color.BLACK;
    private Anchor anchor = Anchor.CenterCenter;

    public TransformExpandCanvas2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TransformExpandCanvas2DAlgorithm(TransformExpandCanvas2DAlgorithm other) {
        super(other);
        this.xAxis = new NumericFunctionExpression(other.xAxis);
        this.yAxis = new NumericFunctionExpression(other.yAxis);
        this.backgroundColor = other.backgroundColor;
        this.anchor = other.anchor;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();
        int wOld = imp.getWidth();
        int hOld = imp.getHeight();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        int wNew = (int) xAxis.apply(wOld, variables);
        int hNew = (int) yAxis.apply(hOld, variables);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(ImageJUtils.expandImageCanvas(imp, backgroundColor, wNew, hNew, anchor)), progressInfo);
    }

    @SetJIPipeDocumentation(name = "X axis", description = "Defines the size of the output canvas")
    @JIPipeParameter("x-axis")
    public NumericFunctionExpression getxAxis() {
        return xAxis;
    }

    @JIPipeParameter("x-axis")
    public void setxAxis(NumericFunctionExpression xAxis) {
        this.xAxis = xAxis;
    }

    @SetJIPipeDocumentation(name = "Y axis", description = "Defines the size of the output canvas")
    @JIPipeParameter("y-axis")
    public NumericFunctionExpression getyAxis() {
        return yAxis;
    }

    @JIPipeParameter("y-axis")
    public void setyAxis(NumericFunctionExpression yAxis) {
        this.yAxis = yAxis;
    }

    @SetJIPipeDocumentation(name = "Background color", description = "The color of the outside canvas")
    @JIPipeParameter("background-color")
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @SetJIPipeDocumentation(name = "Anchor", description = "From which point to expand the canvas")
    @JIPipeParameter("anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @JIPipeParameter("anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }
}
