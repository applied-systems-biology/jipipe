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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.filter;

import ij.ImagePlus;
import ij.process.FloatPolygon;
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
import org.hkijena.jipipe.extensions.imagejdatatypes.util.RoiOutline;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Remove ROI at borders", description = "Removes all ROI that intersect with image borders. Use the 'Border' parameter " +
        "to define a rectangle inside of the image dimensions. If a ROI is not contained within this region, it is removed.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Cleaned ROI")
public class RemoveBorderRoisAlgorithm extends JIPipeIteratingAlgorithm {

    private Margin borderDefinition = new Margin();
    private RoiOutline outline = RoiOutline.ClosedPolygon;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public RemoveBorderRoisAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("ROI", "ROI to be processed", ROIListData.class)
                .addInputSlot("Image", "The reference image", ImagePlusData.class)
                .addOutputSlot("Cleaned ROI", "The cleaned ROI", ROIListData.class, null)
                .seal()
                .build());
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData data = (ROIListData) dataBatch.getInputData("ROI", ROIListData.class, progressInfo).duplicate(progressInfo);
        data.outline(outline);
        ImagePlus reference = dataBatch.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
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

        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Border", description = "Defines the rectangle that is created within the image boundaries separate inside and outside. " +
            "If a ROI intersects with the outside area (meaning that it is not contained within the rectangle), it is removed.")
    @JIPipeParameter("border-definition")
    public Margin getBorderDefinition() {
        return borderDefinition;
    }

    @JIPipeParameter("border-definition")
    public void setBorderDefinition(Margin borderDefinition) {
        this.borderDefinition = borderDefinition;
    }

    @JIPipeDocumentation(name = "Outline method", description = "Determines how ROI are preprocessed to obtain the extreme points (i.e. polygon stops)")
    @JIPipeParameter("outline")
    public RoiOutline getOutline() {
        return outline;
    }

    @JIPipeParameter("outline")
    public void setOutline(RoiOutline outline) {
        this.outline = outline;
    }
}