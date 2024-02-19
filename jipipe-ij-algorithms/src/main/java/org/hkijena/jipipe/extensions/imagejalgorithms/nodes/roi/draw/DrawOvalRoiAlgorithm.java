package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.draw;

import ij.gui.OvalRoi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
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
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.library.roi.Margin;

import java.awt.*;

@SetJIPipeDocumentation(name = "Draw oval ROI", description = "Draws one or multiple oval/ellipse ROI")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", description = "Reference image for the positioning. If not set, the area covered by the existing ROI are used (or width=0, height=0)", optional = true, create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@DefineJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw")
public class DrawOvalRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final ROIProperties roiProperties;

    private Margin.List rectangles = new Margin.List();

    private boolean center = false;

    public DrawOvalRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new ROIProperties();
        rectangles.setCustomInstanceGenerator(this::createNewDefinition);
        rectangles.addNewInstance();
    }

    public DrawOvalRoiAlgorithm(DrawOvalRoiAlgorithm other) {
        super(other);
        this.roiProperties = new ROIProperties(other.roiProperties);
        this.rectangles = new Margin.List(other.rectangles);
        this.center = other.center;
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
            OvalRoi roi = new OvalRoi(area.x, area.y, area.width, area.height);
            roiProperties.applyTo(roi, variables);
            target.add(roi);
        }

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @SetJIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public ROIProperties getRoiProperties() {
        return roiProperties;
    }

    @SetJIPipeDocumentation(name = "Ellipses", description = "The oval ROI to be drawn")
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
}
