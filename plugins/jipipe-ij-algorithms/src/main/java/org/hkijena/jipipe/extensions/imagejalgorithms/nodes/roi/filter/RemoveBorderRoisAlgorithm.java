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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.filter;

import ij.ImagePlus;
import ij.process.FloatPolygon;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
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
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Remove ROI at borders", description = "Removes all ROI that intersect with image borders. Use the 'Border' parameter " +
        "to define a rectangle inside of the image dimensions. If a ROI is not contained within this region, it is removed.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "The ROI to be processed", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "The reference image", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Cleaned ROI", description = "The cleaned ROI", create = true)
public class RemoveBorderRoisAlgorithm extends JIPipeIteratingAlgorithm {

    private Margin borderDefinition = new Margin();
    private RoiOutline outline = RoiOutline.ClosedPolygon;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RemoveBorderRoisAlgorithm(JIPipeNodeInfo info) {
        super(info);
        borderDefinition.getLeft().setExactValue(1);
        borderDefinition.getTop().setExactValue(1);
        borderDefinition.getRight().setExactValue(1);
        borderDefinition.getBottom().setExactValue(1);
        borderDefinition.setAnchor(Anchor.CenterCenter);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public RemoveBorderRoisAlgorithm(RemoveBorderRoisAlgorithm other) {
        super(other);
        this.borderDefinition = new Margin(other.borderDefinition);
        this.outline = other.outline;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) iterationStep.getInputData("ROI", ROIListData.class, progressInfo).duplicate(progressInfo);
        data.outline(outline);
        ImagePlus reference = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        Rectangle inside = borderDefinition.getInsideArea(new Rectangle(0, 0, reference.getWidth(), reference.getHeight()), variables);

        data.removeIf(roi -> {
            FloatPolygon fp = roi.getFloatPolygon();
            for (int i = 0; i < fp.npoints; i++) {
                if (!inside.contains((int) fp.xpoints[i], (int) fp.ypoints[i])) {
                    return true;
                }
            }
            return false;
        });

        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Border", description = "Defines the rectangle that is created within the image boundaries separate inside and outside. " +
            "If a ROI intersects with the outside area (meaning that it is not contained within the rectangle), it is removed.")
    @JIPipeParameter("border-definition")
    public Margin getBorderDefinition() {
        return borderDefinition;
    }

    @JIPipeParameter("border-definition")
    public void setBorderDefinition(Margin borderDefinition) {
        this.borderDefinition = borderDefinition;
    }

    @SetJIPipeDocumentation(name = "Outline method", description = "Determines how ROI are preprocessed to obtain the extreme points (i.e. polygon stops)")
    @JIPipeParameter("outline")
    public RoiOutline getOutline() {
        return outline;
    }

    @JIPipeParameter("outline")
    public void setOutline(RoiOutline outline) {
        this.outline = outline;
    }
}
