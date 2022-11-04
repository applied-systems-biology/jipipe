package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.draw;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

@JIPipeDocumentation(name = "Draw rectangular ROI", description = "Draws one or multiple rectangular ROI. Also supports the drawing of rounded rectangles.")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", description = "Reference image for the positioning. If not set, the area covered by the existing ROI are used (or width=0, height=0)", optional = true, autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw")
public class DrawRectangleRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final ROIProperties roiProperties;

    private Margin.List rectangles = new Margin.List();

    private boolean center = false;

    private int arcWidth = 0;

    private int arcHeight = 0;

    public DrawRectangleRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new ROIProperties();
        rectangles.setCustomInstanceGenerator(this::createNewDefinition);
        rectangles.addNewInstance();
    }

    public DrawRectangleRoiAlgorithm(DrawRectangleRoiAlgorithm other) {
        super(other);
        this.roiProperties = new ROIProperties(other.roiProperties);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        // Generate variables
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        // Collect target and reference
        ROIListData target = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
        if (target == null) {
            target = new ROIListData();
        } else {
            target = new ROIListData(target);
        }
        Rectangle reference;
        ImagePlusData referenceImage = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo);
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
            if(roiProperties.getFillColor().isEnabled()) {
                roi.setFillColor(roiProperties.getFillColor().getContent());
            }
            if(roiProperties.getLineColor().isEnabled()) {
                roi.setStrokeColor(roiProperties.getLineColor().getContent());
            }
        }

        // Output
        dataBatch.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @JIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public ROIProperties getRoiProperties() {
        return roiProperties;
    }

    @JIPipeDocumentation(name = "Rectangles", description = "The rectangular ROI to be drawn")
    @JIPipeParameter("definitions")
    public Margin.List getRectangles() {
        return rectangles;
    }

    @JIPipeParameter("definitions")
    public void setRectangles(Margin.List rectangles) {
        this.rectangles = rectangles;
    }

    @JIPipeDocumentation(name = "Center at location", description = "If enabled, the calculated (x,y) location will be the center of the object")
    @JIPipeParameter("center")
    public boolean isCenter() {
        return center;
    }

    @JIPipeParameter("center")
    public void setCenter(boolean center) {
        this.center = center;
    }

    @JIPipeDocumentation(name = "Arc width", description = "If set to a value larger than zero, a rounded rectangle is created")
    @JIPipeParameter("arc-width")
    public int getArcWidth() {
        return arcWidth;
    }

    @JIPipeParameter("arc-width")
    public void setArcWidth(int arcWidth) {
        this.arcWidth = arcWidth;
    }

    @JIPipeDocumentation(name = "Arc height", description = "If set to a value larger than zero, a rounded rectangle is created")
    @JIPipeParameter("arc-height")
    public int getArcHeight() {
        return arcHeight;
    }

    @JIPipeParameter("arc-height")
    public void setArcHeight(int arcHeight) {
        this.arcHeight = arcHeight;
    }
}
