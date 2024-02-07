package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.draw;

import ij.gui.TextRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontStyleParameter;
import org.hkijena.jipipe.extensions.parameters.library.roi.FixedMargin;

import java.awt.*;
import java.awt.geom.Rectangle2D;

@JIPipeDocumentation(name = "Draw text ROI", description = "Draws a text ROI")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", description = "Reference image for the positioning. If not set, the area covered by the existing ROI are used (or width=0, height=0)", optional = true, autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw")
public class DrawTextRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final ROIProperties roiProperties;
    private JIPipeExpressionParameter text = new JIPipeExpressionParameter("\"your text here\"");

    private FixedMargin location = new FixedMargin();
    private boolean center = false;
    private FontFamilyParameter fontFamily = new FontFamilyParameter();

    private FontStyleParameter fontStyle = FontStyleParameter.Plain;
    private int fontSize = 12;
    private double angle = 0;

    private boolean antialiased = true;


    public DrawTextRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new ROIProperties();
    }

    public DrawTextRoiAlgorithm(DrawTextRoiAlgorithm other) {
        super(other);
        this.roiProperties = new ROIProperties(other.roiProperties);
        this.fontFamily = new FontFamilyParameter(other.fontFamily);
        this.fontSize = other.fontSize;
        this.fontStyle = other.fontStyle;
        this.location = new FixedMargin(other.location);
        this.center = other.center;
        this.angle = other.angle;
        this.antialiased = other.antialiased;
        this.text = new JIPipeExpressionParameter(other.text);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {

        // Generate variables
        ExpressionVariables variables = new ExpressionVariables();
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

        String finalText = text.evaluateToString(variables);
        Font font = fontStyle.toFont(fontFamily, fontSize);

        // Calculate font metrics
        Canvas canvas = new Canvas();
        FontMetrics fontMetrics = canvas.getFontMetrics(font);
        Rectangle2D stringBounds = fontMetrics.getStringBounds(finalText, canvas.getGraphics());

        // Generate location
        Rectangle finalLocation = location.place(stringBounds.getBounds(), reference, variables);
        if (center) {
            finalLocation.x -= finalLocation.width / 2;
            finalLocation.y -= finalLocation.height / 2;
        }

        // Generate items
        TextRoi textRoi = new TextRoi(finalLocation.x, finalLocation.y, finalLocation.width, finalLocation.height, finalText, font);
        textRoi.setAngle(angle);
        textRoi.setAntialiased(antialiased);
        roiProperties.applyTo(textRoi, variables);

        target.add(textRoi);

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }

    @JIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public ROIProperties getRoiProperties() {
        return roiProperties;
    }


    @JIPipeDocumentation(name = "Font family", description = "The font of the text")
    @JIPipeParameter("font-family")
    public FontFamilyParameter getFontFamily() {
        return fontFamily;
    }

    @JIPipeParameter("font-family")
    public void setFontFamily(FontFamilyParameter fontFamily) {
        this.fontFamily = fontFamily;
    }

    @JIPipeDocumentation(name = "Text", description = "Expression that generates the text")
    @JIPipeParameter(value = "text", important = true)
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getText() {
        return text;
    }

    @JIPipeParameter("text")
    public void setText(JIPipeExpressionParameter text) {
        this.text = text;
    }

    @JIPipeDocumentation(name = "Font size", description = "The size of the text")
    @JIPipeParameter("font-size")
    public int getFontSize() {
        return fontSize;
    }

    @JIPipeParameter("font-size")
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    @JIPipeDocumentation(name = "Font style", description = "The style of the text")
    @JIPipeParameter("font-style")
    public FontStyleParameter getFontStyle() {
        return fontStyle;
    }

    @JIPipeParameter("font-style")
    public void setFontStyle(FontStyleParameter fontStyle) {
        this.fontStyle = fontStyle;
    }

    @JIPipeDocumentation(name = "Location", description = "Determines the location of the text relative to the reference image/bounds of existing ROI")
    @JIPipeParameter("location")
    public FixedMargin getLocation() {
        return location;
    }

    @JIPipeParameter("location")
    public void setLocation(FixedMargin location) {
        this.location = location;
    }

    @JIPipeDocumentation(name = "Center at location", description = "If enabled, the calculated (x,y) location will be the center of the text")
    @JIPipeParameter("center")
    public boolean isCenter() {
        return center;
    }

    @JIPipeParameter("center")
    public void setCenter(boolean center) {
        this.center = center;
    }

    @JIPipeDocumentation(name = "Angle", description = "Angle of the text")
    @JIPipeParameter("angle")
    public double getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(double angle) {
        this.angle = angle;
    }

    @JIPipeDocumentation(name = "Antialiased text", description = "If enabled, text will be antialiased")
    @JIPipeParameter("antialiased")
    public boolean isAntialiased() {
        return antialiased;
    }

    @JIPipeParameter("antialiased")
    public void setAntialiased(boolean antialiased) {
        this.antialiased = antialiased;
    }
}
