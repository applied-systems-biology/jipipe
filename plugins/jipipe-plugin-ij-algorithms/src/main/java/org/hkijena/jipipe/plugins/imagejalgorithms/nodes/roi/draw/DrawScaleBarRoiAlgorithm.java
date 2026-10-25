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

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.MeasurementsFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.images.PreprocessingFunction;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.calibration.ScaleBarGenerator;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.OptionalQuantity;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;

import java.awt.*;

@SetJIPipeDocumentation(name = "Draw 2D scale bar ROI", description = "Draws a scale bar ROI. Compared to 'Draw scale bar', this node allows better control over overlay.")
@ConfigureJIPipeNode(function = { PreprocessingFunction.class, MeasurementsFunction.class }, framework = ImageJ1Framework.class, spatialDimensions = { SPATIAL_2D, SPATIAL_3D })
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "Reference image for the positioning. If not set, the area covered by the existing ROI are used (or width=0, height=0)", create = true)
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "ROI", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Collect target and reference
        Roi2dListData target = iterationStep.getInputData("ROI", Roi2dListData.class, progressInfo);
        if (target == null) {
            target = new Roi2dListData();
        } else {
            target = new Roi2dListData(target);
        }
        ImagePlus imp = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo).getDuplicateImage();

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
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

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
        generator.getConfig().setTextColor(textSettings.getFont().getColor());
        generator.getConfig().setFont(textSettings.getFont().toFont());
        generator.getConfig().setHideText(textSettings.isHideLabels());

        Overlay scaleBarOverlay = generator.createScaleBarOverlay();
        for (Roi roi : scaleBarOverlay) {
            target.add(roi);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }


    @SetJIPipeDocumentation(name = "Label settings", description = "The following settings allow you to control the text labels")
    @JIPipeParameter("text-settings")
    public TextSettings getTextSettings() {
        return textSettings;
    }

    @SetJIPipeDocumentation(name = "Bar size (horizontal)", description = "Size of the scale bar. If disabled, a calculated default value is used.")
    @JIPipeParameter("bar-size-horizontal")
    public OptionalQuantity getHorizontalBarSize() {
        return horizontalBarSize;
    }

    @JIPipeParameter("bar-size-horizontal")
    public void setHorizontalBarSize(OptionalQuantity horizontalBarSize) {
        this.horizontalBarSize = horizontalBarSize;
    }

    @SetJIPipeDocumentation(name = "Bar size (vertical)", description = "Size of the scale bar. If disabled, a calculated default value is used.")
    @JIPipeParameter("bar-size-vertical")
    public OptionalQuantity getVerticalBarSize() {
        return verticalBarSize;
    }

    @JIPipeParameter("bar-size-vertical")
    public void setVerticalBarSize(OptionalQuantity verticalBarSize) {
        this.verticalBarSize = verticalBarSize;
    }

    @SetJIPipeDocumentation(name = "Bar thickness (px)", description = "Thickness of the scale bar in pixels.")
    @JIPipeParameter("bar-thickness")
    public int getBarThickness() {
        return barThickness;
    }

    @JIPipeParameter("bar-thickness")
    public void setBarThickness(int barThickness) {
        this.barThickness = barThickness;
    }

    @SetJIPipeDocumentation(name = "Bar color", description = "Color of the bar")
    @JIPipeParameter("bar-color")
    public Color getBarColor() {
        return barColor;
    }

    @JIPipeParameter("bar-color")
    public void setBarColor(Color barColor) {
        this.barColor = barColor;
    }


    @SetJIPipeDocumentation(name = "Background color", description = "If enabled, the background of the bar")
    @JIPipeParameter("background-color")
    public OptionalColorParameter getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(OptionalColorParameter backgroundColor) {
        this.backgroundColor = backgroundColor;
    }


    @SetJIPipeDocumentation(name = "Location", description = "Location of the scale bar")
    @JIPipeParameter("location")
    public ScaleBarGenerator.ScaleBarPosition getLocation() {
        return location;
    }

    @JIPipeParameter("location")
    public void setLocation(ScaleBarGenerator.ScaleBarPosition location) {
        this.location = location;
    }

    @SetJIPipeDocumentation(name = "Draw horizontal scale bar", description = "If enabled, a horizontal scale bar is drawn")
    @JIPipeParameter("draw-horizontal")
    public boolean isDrawHorizontal() {
        return drawHorizontal;
    }

    @JIPipeParameter("draw-horizontal")
    public void setDrawHorizontal(boolean drawHorizontal) {
        this.drawHorizontal = drawHorizontal;
    }

    @SetJIPipeDocumentation(name = "Draw vertical scale bar", description = "If enabled, a vertical scale bar is drawn")
    @JIPipeParameter("draw-vertical")
    public boolean isDrawVertical() {
        return drawVertical;
    }

    @JIPipeParameter("draw-vertical")
    public void setDrawVertical(boolean drawVertical) {
        this.drawVertical = drawVertical;
    }

    /**
     * #1339: migrate the legacy split text-font parameters (text-font,
     * text-font-size, font-style) and the separate text-color into the single
     * {@code text-settings/font} parameter when a pre-migration project is
     * loaded. The nested collection is loaded via flat slash-keys, so the
     * keys are prefixed with {@code text-settings/}. Idempotent: after the next
     * save the legacy keys are gone.
     */
    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        if (node.has("text-settings/text-font") || node.has("text-settings/text-font-size")
                || node.has("text-settings/font-style") || node.has("text-settings/text-color")) {
            textSettings.setFont(JIPipeFontParameter.composeFromLegacy(node,
                    "text-settings/text-font", "text-settings/text-font-size",
                    "text-settings/font-style", "text-settings/text-color", 12));
        }
    }

    public static class TextSettings extends AbstractJIPipeParameterCollection {
        private JIPipeFontParameter font = new JIPipeFontParameter();
        private JIPipeExpressionParameter horizontalLabel = new JIPipeExpressionParameter("D2S(value, 0) + \" \" + unit");

        private JIPipeExpressionParameter verticalLabel = new JIPipeExpressionParameter("D2S(value, 0) + \" \" + unit");

        private boolean hideLabels = false;

        public TextSettings() {
            font.migrateFrom(JIPipeFontParameter.defaultFamily(), 12, Font.BOLD, Color.WHITE);
        }

        public TextSettings(TextSettings other) {
            this.font = new JIPipeFontParameter(other.font);
            this.horizontalLabel = new JIPipeExpressionParameter(other.horizontalLabel);
            this.verticalLabel = new JIPipeExpressionParameter(other.verticalLabel);
            this.hideLabels = other.hideLabels;
        }

        @SetJIPipeDocumentation(name = "Label (horizontal)", description = "Expression that determines the text to be rendered.")
        @JIPipeParameter(value = "label-h", important = true)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Unit", description = "The unit of the scale bar")
        @AddJIPipeExpressionParameterVariable(name = "Value", description = "The value of the scale bar")
        public JIPipeExpressionParameter getHorizontalLabel() {
            return horizontalLabel;
        }

        @JIPipeParameter("label-h")
        public void setHorizontalLabel(JIPipeExpressionParameter horizontalLabel) {
            this.horizontalLabel = horizontalLabel;
        }

        @SetJIPipeDocumentation(name = "Label (vertical)", description = "Expression that determines the text to be rendered.")
        @JIPipeParameter(value = "label-v", important = true)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Unit", description = "The unit of the scale bar")
        @AddJIPipeExpressionParameterVariable(name = "Value", description = "The value of the scale bar")
        public JIPipeExpressionParameter getVerticalLabel() {
            return verticalLabel;
        }

        @JIPipeParameter("label-v")
        public void setVerticalLabel(JIPipeExpressionParameter verticalLabel) {
            this.verticalLabel = verticalLabel;
        }

        @SetJIPipeDocumentation(name = "Hide labels", description = "If enabled, all labels will be hidden")
        @JIPipeParameter("hide-labels")
        public boolean isHideLabels() {
            return hideLabels;
        }

        @JIPipeParameter("hide-labels")
        public void setHideLabels(boolean hideLabels) {
            this.hideLabels = hideLabels;
        }

        @SetJIPipeDocumentation(name = "Font", description = "Font (family, size, style and color) of the text")
        @JIPipeParameter("font")
        public JIPipeFontParameter getFont() {
            return font;
        }

        @JIPipeParameter("font")
        public void setFont(JIPipeFontParameter font) {
            this.font = font;
        }
    }
}
