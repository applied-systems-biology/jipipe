package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.calibration;

import ij.ImagePlus;
import ij.gui.*;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.OptionalDefaultExpressionParameter;
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
import java.awt.geom.Rectangle2D;

@JIPipeDocumentation(name = "Draw scale bar", description = "Draws a scale bar onto the image (via an overlay ROI)")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze\nTools", aliasName = "Scale Bar...")
public class DrawScaleBarAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalQuantity barSize = new OptionalQuantity(new Quantity(1, "µm"), false);
    private Quantity barThickness = new Quantity(4, "px");
    private Color barColor = Color.WHITE;
    private OptionalColorParameter backgroundColor = new OptionalColorParameter(Color.BLACK, false);
    private FixedMargin location = new FixedMargin(Anchor.BottomRight);
    private boolean drawHorizontal = true;
    private boolean drawVertical = false;

    private final TextSettings textSettings;

    public DrawScaleBarAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.textSettings = new TextSettings();
        registerSubParameter(textSettings);
        location.getLeft().setExactValue(10);
        location.getTop().setExactValue(10);
        location.getRight().setExactValue(10);
        location.getBottom().setExactValue(10);
    }

    public DrawScaleBarAlgorithm(DrawScaleBarAlgorithm other) {
        super(other);
        this.textSettings = new TextSettings(other.textSettings);
        registerSubParameter(textSettings);
        this.barSize = new OptionalQuantity(other.barSize);
        this.barThickness = new Quantity(other.barThickness);
        this.barColor = other.barColor;
        this.backgroundColor = new OptionalColorParameter(other.backgroundColor);
        this.location = new FixedMargin(other.location);
        this.drawHorizontal = other.drawHorizontal;
        this.drawVertical = other.drawVertical;
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

        if(textSettings.text.isEnabled()) {
            {
                variables.set("unit", cal.getXUnit());
                variables.set("value", calculatedSizeWidth * cal.pixelWidth);
                renderedTextH = textSettings.text.getContent().evaluateToString(variables);
            }
            {
                variables.set("unit", cal.getYUnit());
                variables.set("value", calculatedSizeHeight * cal.pixelHeight);
                renderedTextV = textSettings.text.getContent().evaluateToString(variables);
            }
        }
        else {
            renderedTextH = null;
            renderedTextV = null;
        }

        // Font measurements
        Font font = textSettings.fontStyle.toFont(textSettings.fontFamily, textSettings.fontSize);
        Canvas canvas = new Canvas();
        FontMetrics fontMetrics = canvas.getFontMetrics(font);

        Rectangle2D stringBoundsH = fontMetrics.getStringBounds(renderedTextH, canvas.getGraphics());
        Rectangle2D stringBoundsV = fontMetrics.getStringBounds(renderedTextV, canvas.getGraphics());

        // Generate ROI at their base coordinates
        ROIListData horizontalOverlay = new ROIListData();
        ROIListData verticalOverlay = new ROIListData();

        if(drawHorizontal) {
            drawHorizontalBarBase(calculatedSizeWidth, calculatedThickness, renderedTextH, font, stringBoundsH, horizontalOverlay);
        }
        if(drawVertical) {
            drawVerticalBarBase(calculatedSizeHeight, calculatedThickness, renderedTextV, font, stringBoundsV, verticalOverlay);
        }

        // Combine rois
        ROIListData combined = new ROIListData();
        combined.addAll(horizontalOverlay);
        combined.addAll(verticalOverlay);
        if(backgroundColor.isEnabled()) {
            if(!horizontalOverlay.isEmpty()) {
                ShapeRoi roi = new ShapeRoi(horizontalOverlay.getBounds());
                roi.setName("Background");
                roi.setFillColor(backgroundColor.getContent());
                combined.add(0, roi);
            }
            if(!verticalOverlay.isEmpty()) {
                ShapeRoi roi = new ShapeRoi(verticalOverlay.getBounds());
                roi.setName("Background");
                roi.setFillColor(backgroundColor.getContent());
                combined.add(0, roi);
            }
        }

        // Placement module
        {
            Rectangle bounds = combined.getBounds();
            Rectangle placed = location.place(bounds, new Rectangle(0, 0, imp.getWidth(), imp.getHeight()), variables);
            int dx = placed.x - bounds.x;
            int dy = placed.y - bounds.y;
            for (Roi roi : combined) {
                roi.setLocation(roi.getXBase() + dx, roi.getYBase() + dy);
            }
        }

        // Write overlay
        Overlay overlay = imp.getOverlay();
        if(overlay == null) {
            overlay = new Overlay();
            imp.setOverlay(overlay);
        }
        for (Roi roi : combined) {
            overlay.add(roi);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(imp), progressInfo);
    }

    private void drawVerticalBarBase(double calculatedSizeHeight, double calculatedThickness, String renderedTextV, Font font, Rectangle2D stringBoundsV, ROIListData verticalOverlay) {
        TextRoi textRoi = null;
        ShapeRoi bar;

        boolean textLeft;
        switch (location.getAnchor()) {
            case TopLeft:
            case CenterLeft:
            case BottomLeft:
                textLeft = true;
                break;
            default:
                textLeft = false;
        }

        double textY = stringBoundsV.getWidth();

        if(textLeft) {
            if(renderedTextV != null) {
                textRoi = new TextRoi(renderedTextV, 0, 0, font);
            }
            bar = new ShapeRoi(new Rectangle2D.Double(stringBoundsV.getHeight(), textY, calculatedThickness, calculatedSizeHeight));
        }
        else {
            if(renderedTextV != null) {
                textRoi = new TextRoi(renderedTextV,stringBoundsV.getHeight(), textY, font);
            }
            bar = new ShapeRoi(new Rectangle2D.Double(0, 0, calculatedThickness, calculatedSizeHeight));
        }

        // Change properties and add
        if(textRoi != null) {
            textRoi.setName("[Scale bar] " + renderedTextV + " (text)");
            textRoi.setStrokeColor(textSettings.textColor);
            textRoi.setAngle(90);
            textRoi.setFillColor(Color.ORANGE);
            verticalOverlay.add(textRoi);
        }
        {
            bar.setName("[Scale bar] " + renderedTextV + " (bar)");
            bar.setFillColor(barColor);
            verticalOverlay.add(bar);
        }
    }

    private void drawHorizontalBarBase(double calculatedSizeWidth, double calculatedThickness, String renderedTextH, Font font, Rectangle2D stringBoundsH, ROIListData horizontalOverlay) {
        TextRoi textRoi = null;
        ShapeRoi bar;

        boolean textAbove;
        if(drawVertical) {
            switch (location.getAnchor()) {
                case TopLeft:
                case TopCenter:
                case TopRight:
                    textAbove = true;
                    break;
                default:
                    textAbove = false;
                    break;
            }
        }
        else {
            switch (location.getAnchor()) {
                case TopLeft:
                case TopCenter:
                case TopRight:
                    textAbove = false;
                    break;
                default:
                    textAbove = true;
                    break;
            }
        }

        if(textAbove) {
            if(renderedTextH != null)
                textRoi = new TextRoi(renderedTextH, calculatedSizeWidth / 2 - stringBoundsH.getWidth() / 2, 0, font);
            bar = new ShapeRoi(new Rectangle2D.Double(0, stringBoundsH.getHeight(), calculatedSizeWidth, calculatedThickness));
        }
        else {
            if(renderedTextH != null)
                textRoi = new TextRoi(renderedTextH, calculatedSizeWidth / 2 - stringBoundsH.getWidth() / 2, stringBoundsH.getHeight(), font);
            bar = new ShapeRoi(new Rectangle2D.Double(0, 0, calculatedSizeWidth, calculatedThickness));
        }

        // Change properties and add
        if(textRoi != null) {
            textRoi.setName("[Scale bar] " + renderedTextH + " (text)");
            textRoi.setStrokeColor(textSettings.textColor);
            horizontalOverlay.add(textRoi);
        }
        {
            bar.setName("[Scale bar] " + renderedTextH + " (bar)");
            bar.setFillColor(barColor);
            horizontalOverlay.add(bar);
        }
    }

    private double quantityToPixels(Quantity quantity, String unit, double pixelSize) {
        if(StringUtils.isNullOrEmpty(quantity.getUnit()) || "px".equals(quantity.getUnit()) || "pixel".equals(quantity.getUnit()) || "pixels".equals(quantity.getUnit())) {
            return quantity.getValue();
        }
        else {
            return quantity.convertTo(unit).getValue() / pixelSize;
        }
    }

    @JIPipeDocumentation(name = "Label settings", description = "The following settings allow you to control the text labels")
    @JIPipeParameter("text-settings")
    public TextSettings getTextSettings() {
        return textSettings;
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

    @JIPipeDocumentation(name = "Bar color", description = "Color of the bar")
    @JIPipeParameter("bar-color")
    public Color getBarColor() {
        return barColor;
    }

    @JIPipeParameter("bar-color")
    public void setBarColor(Color barColor) {
        this.barColor = barColor;
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



    @JIPipeDocumentation(name = "Location", description = "Allows to determine the location of the scale bar relative to the image extents")
    @JIPipeParameter("location")
    public FixedMargin getLocation() {
        return location;
    }

    @JIPipeParameter("location")
    public void setLocation(FixedMargin location) {
        this.location = location;
    }

    @JIPipeDocumentation(name = "Draw horizontal scale bar", description = "If enabled, a horizontal scale bar is drawn")
    @JIPipeParameter("draw-horizontal")
    public boolean isDrawHorizontal() {
        return drawHorizontal;
    }

    @JIPipeParameter("draw-horizontal")
    public void setDrawHorizontal(boolean drawHorizontal) {
        this.drawHorizontal = drawHorizontal;
    }

    @JIPipeDocumentation(name = "Draw vertical scale bar", description = "If enabled, a vertical scale bar is drawn")
    @JIPipeParameter("draw-vertical")
    public boolean isDrawVertical() {
        return drawVertical;
    }

    @JIPipeParameter("draw-vertical")
    public void setDrawVertical(boolean drawVertical) {
        this.drawVertical = drawVertical;
    }

    private int computeDefaultBarHeight(ImagePlus imp) {
        double imageHeight = imp.getHeight();
        double vBarHeight = 0;

        vBarHeight = (80.0);
        if (vBarHeight>0.67*imageHeight)
            // If 80 pixels is too much, do 2/3 of the image.
            vBarHeight = 0.67*imageHeight;
        if (vBarHeight>5.0)
            // If the resulting size is larger than 5 units, round the value.
            vBarHeight = (int) vBarHeight;
        return (int) vBarHeight;
    }

    private int computeDefaultBarWidth(ImagePlus imp) {
        double imageWidth = imp.getWidth();
        double hBarWidth;

        // If the bar is of negative width or too wide for the image,
        // set the bar width to 80 pixels.
        hBarWidth = (80.0);
        if (hBarWidth>0.67*imageWidth)
            // If 80 pixels is too much, do 2/3 of the image.
            hBarWidth = 0.67*imageWidth;
        if (hBarWidth>5.0)
            // If the resulting size is larger than 5 units, round the value.
            hBarWidth = (int) hBarWidth;

        return (int) hBarWidth;
    }

    public static class TextSettings extends AbstractJIPipeParameterCollection {
        private FontFamilyParameter fontFamily = new FontFamilyParameter();
        private int fontSize = 12;
        private FontStyleParameter fontStyle = FontStyleParameter.Bold;
        private OptionalDefaultExpressionParameter text = new OptionalDefaultExpressionParameter(true,"value + \" \" + unit");
        private Color textColor = Color.WHITE;

        public TextSettings() {
        }

        public TextSettings(TextSettings other) {
            this.fontFamily = new FontFamilyParameter(other.fontFamily);
            this.fontSize = other.fontSize;
            this.fontStyle = other.fontStyle;
            this.textColor = other.textColor;
            this.text = new OptionalDefaultExpressionParameter(other.text);
        }

        @JIPipeDocumentation(name = "Text", description = "Expression that determines the text to be rendered. If disabled, no text is generated.")
        @JIPipeParameter(value = "text-expression", important = true)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Unit", description = "The unit of the scale bar")
        @ExpressionParameterSettingsVariable(name = "Value", description = "The value of the scale bar")
        public OptionalDefaultExpressionParameter getText() {
            return text;
        }

        @JIPipeParameter("text-expression")
        public void setText(OptionalDefaultExpressionParameter text) {
            this.text = text;
        }

        @JIPipeDocumentation(name = "Text color", description = "The color of the text")
        @JIPipeParameter("text-color")
        public Color getTextColor() {
            return textColor;
        }

        @JIPipeParameter("text-color")
        public void setTextColor(Color textColor) {
            this.textColor = textColor;
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
    }
}
