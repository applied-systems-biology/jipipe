package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.draw;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
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
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.ij1.calibration.ScaleBarGenerator;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontStyleParameter;
import org.hkijena.jipipe.extensions.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.extensions.parameters.library.quantities.Quantity;

import java.awt.*;

@JIPipeDocumentation(name = "Draw scale bar ROI", description = "Draws a scale bar ROI. Compared to 'Draw scale bar', this node allows better control over overlay.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Calibration")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", description = "Reference image for the positioning. If not set, the area covered by the existing ROI are used (or width=0, height=0)", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Analyze\nTools", aliasName = "Scale Bar... (as ROI)")
public class DrawScaleBarRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final TextSettings textSettings;
    private OptionalQuantity horizontalBarSize = new OptionalQuantity(new Quantity(1, "µm"), false);
    private OptionalQuantity verticalBarSize = new OptionalQuantity(new Quantity(1, "µm"), false);
    private int barThickness = 4;
    private Color barColor = Color.WHITE;
    private OptionalColorParameter backgroundColor = new OptionalColorParameter(Color.BLACK, false);
    private ScaleBarGenerator.ScaleBarPosition location = ScaleBarGenerator.ScaleBarPosition.LowerRight;
    private boolean drawHorizontal = true;
    private boolean drawVertical = false;

    public DrawScaleBarRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.textSettings = new TextSettings();
        registerSubParameter(textSettings);
    }

    public DrawScaleBarRoiAlgorithm(DrawScaleBarRoiAlgorithm other) {
        super(other);
        this.textSettings = new TextSettings(other.textSettings);
        registerSubParameter(textSettings);
        this.horizontalBarSize = new OptionalQuantity(other.horizontalBarSize);
        this.verticalBarSize = new OptionalQuantity(other.verticalBarSize);
        this.barThickness = other.barThickness;
        this.barColor = other.barColor;
        this.backgroundColor = new OptionalColorParameter(other.backgroundColor);
        this.location = other.location;
        this.drawHorizontal = other.drawHorizontal;
        this.drawVertical = other.drawVertical;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        // Collect target and reference
        ROIListData target = dataBatch.getInputData("ROI", ROIListData.class, progressInfo);
        if (target == null) {
            target = new ROIListData();
        } else {
            target = new ROIListData(target);
        }
        ImagePlus imp = dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo).getDuplicateImage();

        ScaleBarGenerator generator = new ScaleBarGenerator(imp);
        Calibration cal = imp.getCalibration();

        // Generate bar width (in units)
        generator.computeDefaultBarWidth();
        if (horizontalBarSize.isEnabled()) {
            generator.getConfig().sethBarWidth(horizontalBarSize.getContent().convertTo(cal.getXUnit()).getValue());
        }
        if (verticalBarSize.isEnabled()) {
            generator.getConfig().setvBarHeight(verticalBarSize.getContent().convertTo(cal.getYUnit()).getValue());
        }

        // Render text
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        if (!generator.getConfig().isHideText()) {
            variables.set("unit", cal.getXUnit());
            variables.set("value", generator.getConfig().gethBarWidth());
            generator.getConfig().sethLabel(textSettings.horizontalLabel.evaluateToString(variables));
        }
        if (!generator.getConfig().isHideText()) {
            variables.set("unit", cal.getYUnit());
            variables.set("value", generator.getConfig().getvBarHeight());
            generator.getConfig().setvLabel(textSettings.verticalLabel.evaluateToString(variables));
        }

        // Other settings
        generator.getConfig().setShowVertical(drawVertical);
        generator.getConfig().setShowHorizontal(drawHorizontal);
        generator.getConfig().setLocation(location);
        generator.getConfig().setBarThicknessInPixels(barThickness);
        generator.getConfig().setBarColor(barColor);
        generator.getConfig().setBackgroundColor(backgroundColor.isEnabled() ? backgroundColor.getContent() : null);
        generator.getConfig().setTextColor(textSettings.getTextColor());
        generator.getConfig().setFontFamily(textSettings.getFontFamily());
        generator.getConfig().setFontSize(textSettings.getFontSize());
        generator.getConfig().setFontStyle(textSettings.getFontStyle());
        generator.getConfig().setHideText(textSettings.isHideLabels());

        Overlay scaleBarOverlay = generator.createScaleBarOverlay();
        for (Roi roi : scaleBarOverlay) {
            target.add(roi);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }


    @JIPipeDocumentation(name = "Label settings", description = "The following settings allow you to control the text labels")
    @JIPipeParameter("text-settings")
    public TextSettings getTextSettings() {
        return textSettings;
    }

    @JIPipeDocumentation(name = "Bar size (horizontal)", description = "Size of the scale bar. If disabled, a calculated default value is used.")
    @JIPipeParameter("bar-size-horizontal")
    public OptionalQuantity getHorizontalBarSize() {
        return horizontalBarSize;
    }

    @JIPipeParameter("bar-size-horizontal")
    public void setHorizontalBarSize(OptionalQuantity horizontalBarSize) {
        this.horizontalBarSize = horizontalBarSize;
    }

    @JIPipeDocumentation(name = "Bar size (vertical)", description = "Size of the scale bar. If disabled, a calculated default value is used.")
    @JIPipeParameter("bar-size-vertical")
    public OptionalQuantity getVerticalBarSize() {
        return verticalBarSize;
    }

    @JIPipeParameter("bar-size-vertical")
    public void setVerticalBarSize(OptionalQuantity verticalBarSize) {
        this.verticalBarSize = verticalBarSize;
    }

    @JIPipeDocumentation(name = "Bar thickness (px)", description = "Thickness of the scale bar in pixels.")
    @JIPipeParameter("bar-thickness")
    public int getBarThickness() {
        return barThickness;
    }

    @JIPipeParameter("bar-thickness")
    public void setBarThickness(int barThickness) {
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


    @JIPipeDocumentation(name = "Location", description = "Location of the scale bar")
    @JIPipeParameter("location")
    public ScaleBarGenerator.ScaleBarPosition getLocation() {
        return location;
    }

    @JIPipeParameter("location")
    public void setLocation(ScaleBarGenerator.ScaleBarPosition location) {
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

    public static class TextSettings extends AbstractJIPipeParameterCollection {
        private FontFamilyParameter fontFamily = new FontFamilyParameter();
        private int fontSize = 12;
        private FontStyleParameter fontStyle = FontStyleParameter.Bold;
        private DefaultExpressionParameter horizontalLabel = new DefaultExpressionParameter("D2S(value, 0) + \" \" + unit");

        private DefaultExpressionParameter verticalLabel = new DefaultExpressionParameter("D2S(value, 0) + \" \" + unit");
        private Color textColor = Color.WHITE;

        private boolean hideLabels = false;

        public TextSettings() {
        }

        public TextSettings(TextSettings other) {
            this.fontFamily = new FontFamilyParameter(other.fontFamily);
            this.fontSize = other.fontSize;
            this.fontStyle = other.fontStyle;
            this.textColor = other.textColor;
            this.horizontalLabel = new DefaultExpressionParameter(other.horizontalLabel);
            this.verticalLabel = new DefaultExpressionParameter(other.verticalLabel);
            this.hideLabels = other.hideLabels;
        }

        @JIPipeDocumentation(name = "Label (horizontal)", description = "Expression that determines the text to be rendered.")
        @JIPipeParameter(value = "label-h", important = true)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Unit", description = "The unit of the scale bar")
        @ExpressionParameterSettingsVariable(name = "Value", description = "The value of the scale bar")
        public DefaultExpressionParameter getHorizontalLabel() {
            return horizontalLabel;
        }

        @JIPipeParameter("label-h")
        public void setHorizontalLabel(DefaultExpressionParameter horizontalLabel) {
            this.horizontalLabel = horizontalLabel;
        }

        @JIPipeDocumentation(name = "Label (vertical)", description = "Expression that determines the text to be rendered.")
        @JIPipeParameter(value = "label-v", important = true)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Unit", description = "The unit of the scale bar")
        @ExpressionParameterSettingsVariable(name = "Value", description = "The value of the scale bar")
        public DefaultExpressionParameter getVerticalLabel() {
            return verticalLabel;
        }

        @JIPipeParameter("label-v")
        public void setVerticalLabel(DefaultExpressionParameter verticalLabel) {
            this.verticalLabel = verticalLabel;
        }

        @JIPipeDocumentation(name = "Hide labels", description = "If enabled, all labels will be hidden")
        @JIPipeParameter("hide-labels")
        public boolean isHideLabels() {
            return hideLabels;
        }

        @JIPipeParameter("hide-labels")
        public void setHideLabels(boolean hideLabels) {
            this.hideLabels = hideLabels;
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
