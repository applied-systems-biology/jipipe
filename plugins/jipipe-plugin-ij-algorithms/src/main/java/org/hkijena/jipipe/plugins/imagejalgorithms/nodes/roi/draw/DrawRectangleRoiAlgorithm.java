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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.draw;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;
import org.hkijena.jipipe.plugins.parameters.library.roi.Margin;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

@SetJIPipeDocumentation(name = "Draw rectangular ROI", description = "Draws one or multiple rectangular ROI. Also supports the drawing of rounded rectangles.")
@AddJIPipeInputSlot(value = ROIListData.class, name = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "Reference image for the positioning. If not set, the area covered by the existing ROI are used (or width=0, height=0)", optional = true, create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw")
public class DrawRectangleRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final VisualLocationROIProperties roiProperties;

    private Margin.List rectangles = new Margin.List();

    private boolean center = false;

    private int arcWidth = 0;

    private int arcHeight = 0;

    public DrawRectangleRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualLocationROIProperties();
        rectangles.setCustomInstanceGenerator(this::createNewDefinition);
        rectangles.addNewInstance();
    }

    public DrawRectangleRoiAlgorithm(DrawRectangleRoiAlgorithm other) {
        super(other);
        this.roiProperties = new VisualLocationROIProperties(other.roiProperties);
        this.rectangles = new Margin.List(other.rectangles);
        this.center = other.center;
        this.arcWidth = other.arcWidth;
        this.arcHeight = other.arcHeight;
    }

    private Margin createNewDefinition() {
        Margin margin = new Margin(Anchor.TopLeft);
        margin.getLeft().setExactValue(0);
        margin.getTop().setExactValue(0);
        margin.getWidth().setExactValue(100);
        margin.getHeight().setExactValue(100);
        return margin;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Generate variables
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        // Collect target and reference
        ROIListData target = iterationStep.getInputData("ROI", ROIListData.class, progressInfo);
        if (target == null) {
            target = new ROIListData();
        } else {
            target = new ROIListData(target);
        }
        Rectangle reference;
        ImagePlusData referenceImage = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);
        if (referenceImage != null) {
            reference = new Rectangle(0, 0, referenceImage.getWidth(), referenceImage.getHeight());
        } else {
            reference = target.getBounds();
        }

        // Generate items
        for (Margin rectangle : rectangles) {
            Rectangle area = rectangle.getInsideArea(reference, variables);
            if (center) {
                area.x -= area.width / 2;
                area.y -= area.height / 2;
            }
            if (arcWidth <= 0 && arcHeight <= 0) {
                ShapeRoi roi = new ShapeRoi(area);
                roiProperties.applyTo(roi, variables);
                target.add(roi);
            } else {
                RoundRectangle2D rectangle2D = new RoundRectangle2D.Double(area.x, area.y, area.width, area.height, arcWidth, arcHeight);
                ShapeRoi roi = new ShapeRoi(rectangle2D);
                roiProperties.applyTo(roi, variables);
                target.add(roi);
            }
        }

        for (Roi roi : target) {
            roi.setName(roiProperties.getRoiName().evaluateToString(variables));
            roi.setStrokeWidth(roiProperties.getLineWidth());
            roi.setPosition(roiProperties.getPositionC(), roiProperties.getPositionZ(), roiProperties.getPositionT());
            if (roiProperties.getFillColor().isEnabled()) {
                roi.setFillColor(roiProperties.getFillColor().getContent());
            }
            if (roiProperties.getLineColor().isEnabled()) {
                roi.setStrokeColor(roiProperties.getLineColor().getContent());
            }
        }

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @SetJIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public VisualLocationROIProperties getRoiProperties() {
        return roiProperties;
    }

    @SetJIPipeDocumentation(name = "Rectangles", description = "The rectangular ROI to be drawn")
    @JIPipeParameter("definitions")
    public Margin.List getRectangles() {
        return rectangles;
    }

    @JIPipeParameter("definitions")
    public void setRectangles(Margin.List rectangles) {
        this.rectangles = rectangles;
    }

    @SetJIPipeDocumentation(name = "Center at location", description = "If enabled, the calculated (x,y) location will be the center of the object")
    @JIPipeParameter("center")
    public boolean isCenter() {
        return center;
    }

    @JIPipeParameter("center")
    public void setCenter(boolean center) {
        this.center = center;
    }

    @SetJIPipeDocumentation(name = "Arc width", description = "If set to a value larger than zero, a rounded rectangle is created")
    @JIPipeParameter("arc-width")
    public int getArcWidth() {
        return arcWidth;
    }

    @JIPipeParameter("arc-width")
    public void setArcWidth(int arcWidth) {
        this.arcWidth = arcWidth;
    }

    @SetJIPipeDocumentation(name = "Arc height", description = "If set to a value larger than zero, a rounded rectangle is created")
    @JIPipeParameter("arc-height")
    public int getArcHeight() {
        return arcHeight;
    }

    @JIPipeParameter("arc-height")
    public void setArcHeight(int arcHeight) {
        this.arcHeight = arcHeight;
    }
}
