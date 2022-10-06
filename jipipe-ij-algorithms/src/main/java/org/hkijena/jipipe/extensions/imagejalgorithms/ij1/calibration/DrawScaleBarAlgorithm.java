package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.calibration;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontStyleParameter;
import org.hkijena.jipipe.extensions.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.parameters.library.roi.FixedMargin;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

@JIPipeDocumentation(name = "Draw scale bar", description = "Draws a scale bar onto the image (via an overlay ROI)")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze\nTools", aliasName = "Scale Bar...")
public class DrawScaleBarAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalQuantity barSize = new OptionalQuantity(new Quantity(1, "µm"), false);
    private Quantity barThickness = new Quantity(4, "px");
    private FontFamilyParameter fontFamily = new FontFamilyParameter();
    private int fontSize = 12;

    private FontStyleParameter fontStyle = FontStyleParameter.Bold;
    private Color foregroundColor = Color.WHITE;
    private OptionalColorParameter backgroundColor = new OptionalColorParameter(Color.BLACK, false);
    private DefaultExpressionParameter text = new DefaultExpressionParameter("value + \" \" + unit");

    private FixedMargin location = new FixedMargin(Anchor.BottomRight);

    public DrawScaleBarAlgorithm(JIPipeNodeInfo info) {
        super(info);
        location.getLeft().setExactValue(10);
        location.getTop().setExactValue(10);
        location.getRight().setExactValue(10);
        location.getBottom().setExactValue(10);
    }

    public DrawScaleBarAlgorithm(DrawScaleBarAlgorithm other) {
        super(other);
        this.barSize = new OptionalQuantity(other.barSize);
        this.barThickness = new Quantity(other.barThickness);
        this.fontFamily = new FontFamilyParameter(other.fontFamily);
        this.fontSize = other.fontSize;
        this.foregroundColor = other.foregroundColor;
        this.backgroundColor = new OptionalColorParameter(other.backgroundColor);
        this.text = new DefaultExpressionParameter(other.text);
        this.location = new FixedMargin(other.location);
        this.fontStyle = other.fontStyle;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus imp =dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        Calibration cal = imp.getCalibration();

        // Render bar sizes
        double calculatedSizeWidth;
        double calculatedSizeHeight;
        double calculatedThickness = quantityToPixels(barThickness, cal.getXUnit(), cal.pixelWidth);
        if(barSize.isEnabled()) {
            calculatedSizeWidth = quantityToPixels(barSize.getContent(), cal.getXUnit(), cal.pixelWidth);
            calculatedSizeHeight = quantityToPixels(barSize.getContent(), cal.getYUnit(), cal.pixelHeight);
        }
        else {
            calculatedSizeWidth = computeDefaultBarWidth(imp);
            calculatedSizeHeight = computeDefaultBarHeight(imp);
        }

        // Render labels
        String renderedTextH;
        String renderedTextV;

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        {
            variables.set("unit", cal.getXUnit());
            variables.set("value", calculatedSizeWidth / cal.pixelWidth);
            renderedTextH = text.evaluateToString(variables);
        }
        {
            variables.set("unit", cal.getYUnit());
            variables.set("value", calculatedSizeHeight / cal.pixelHeight);
            renderedTextV = text.evaluateToString(variables);
        }


        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp), progressInfo);
    }

    private double quantityToPixels(Quantity quantity, String unit, double pixelSize) {
        if(StringUtils.isNullOrEmpty(quantity.getUnit()) || "px".equals(quantity.getUnit()) || "pixel".equals(quantity.getUnit()) || "pixels".equals(quantity.getUnit())) {
            return quantity.getValue();
        }
        else {
            return quantity.convertTo(unit).getValue() * pixelSize;
        }
    }

    @JIPipeDocumentation(name = "Bar size", description = "Size of the scale bar. If disabled, a calculated default value is used.")
    @JIPipeParameter("bar-size")
    public OptionalQuantity getBarSize() {
        return barSize;
    }

    @JIPipeParameter("bar-size")
    public void setBarSize(OptionalQuantity barSize) {
        this.barSize = barSize;
    }

    @JIPipeDocumentation(name = "Bar thickness", description = "Thickness of the scale bar.")
    @JIPipeParameter("bar-thickness")
    public Quantity getBarThickness() {
        return barThickness;
    }

    @JIPipeParameter("bar-thickness")
    public void setBarThickness(Quantity barThickness) {
        this.barThickness = barThickness;
    }

    @JIPipeDocumentation(name = "Font", description = "Font of the text")
    @JIPipeParameter("text-font")
    public FontFamilyParameter getFontFamily() {
        return fontFamily;
    }

    @JIPipeParameter("text-font")
    public void setFontFamily(FontFamilyParameter fontFamily) {
        this.fontFamily = fontFamily;
    }

    @JIPipeDocumentation(name = "Font size", description = "Size of the text")
    @JIPipeParameter("text-font-size")
    public int getFontSize() {
        return fontSize;
    }

    @JIPipeParameter("text-font-size")
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    @JIPipeDocumentation(name = "Font style", description = "The style of the font")
    @JIPipeParameter("font-style")
    public FontStyleParameter getFontStyle() {
        return fontStyle;
    }

    @JIPipeParameter("font-style")
    public void setFontStyle(FontStyleParameter fontStyle) {
        this.fontStyle = fontStyle;
    }

    @JIPipeDocumentation(name = "Color", description = "Color of the bar")
    @JIPipeParameter("foreground-color")
    public Color getForegroundColor() {
        return foregroundColor;
    }

    @JIPipeParameter("foreground-color")
    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    @JIPipeDocumentation(name = "Background color", description = "If enabled, the background of the bar")
    @JIPipeParameter("background-color")
    public OptionalColorParameter getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(OptionalColorParameter backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @JIPipeDocumentation(name = "Text", description = "Expression that determines the text to be rendered")
    @JIPipeParameter("text-expression")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Unit", description = "The unit of the scale bar")
    @ExpressionParameterSettingsVariable(name = "Value", description = "The value of the scale bar")
    public DefaultExpressionParameter getText() {
        return text;
    }

    @JIPipeParameter("text-expression")
    public void setText(DefaultExpressionParameter text) {
        this.text = text;
    }

    @JIPipeDocumentation(name = "Location", description = "Allows to determine the location of the scale bar relative to the image extents")
    @JIPipeParameter("location")
    public FixedMargin getLocation() {
        return location;
    }

    @JIPipeParameter("location")
    public void setLocation(FixedMargin location) {
        this.location = location;
    }

    private int computeDefaultBarHeight(ImagePlus imp) {
        Calibration cal = imp.getCalibration();
        double mag = 1.0;
        double pixelHeight = cal.pixelHeight;
        if (pixelHeight==0.0)
            pixelHeight = 1.0;
        double imageHeight = imp.getHeight()*pixelHeight;
        double vBarHeight = 0;

        vBarHeight = (80.0*pixelHeight)/mag;
        if (vBarHeight>0.67*imageHeight)
            // If 80 pixels is too much, do 2/3 of the image.
            vBarHeight = 0.67*imageHeight;
        if (vBarHeight>5.0)
            // If the resulting size is larger than 5 units, round the value.
            vBarHeight = (int) vBarHeight;
        return (int) vBarHeight;
    }

    private int computeDefaultBarWidth(ImagePlus imp) {
        Calibration cal = imp.getCalibration();
        double mag = 1.0;
        double pixelWidth = cal.pixelWidth;
        if (pixelWidth==0.0)
            pixelWidth = 1.0;
        double imageWidth = imp.getWidth()*pixelWidth;
        double hBarWidth = 0;

        // If the bar is of negative width or too wide for the image,
        // set the bar width to 80 pixels.
        hBarWidth = (80.0*pixelWidth)/mag;
        if (hBarWidth>0.67*imageWidth)
            // If 80 pixels is too much, do 2/3 of the image.
            hBarWidth = 0.67*imageWidth;
        if (hBarWidth>5.0)
            // If the resulting size is larger than 5 units, round the value.
            hBarWidth = (int) hBarWidth;

        return (int) hBarWidth;
    }
}
