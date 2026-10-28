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
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.taxonomy.frameworks.ImageJ1Framework;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.rois.RoiFiltersFunction;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.Roi2dListData;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleParameter;
import org.hkijena.jipipe.plugins.parameters.library.roi.FixedMargin;
import org.hkijena.jipipe.plugins.parameters.library.roi.InnerMargin;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;

import java.awt.*;
import java.awt.geom.Rectangle2D;

@SetJIPipeDocumentation(name = "Draw 2D text ROI", description = "Creates a 2D text ROI and adds it to an existing 2D ROI list, or starts a new list if none is provided. It positions the text relative to an optional reference image or the current ROI bounds, with font, angle, and optional background settings, and outputs the updated 2D ROI list.")
@AddJIPipeInputSlot(value = Roi2dListData.class, name = "ROI", description = "Optional existing list of ROI. The new ROI will be appended to it.", optional = true, create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", description = "Reference image for the positioning. If not set, the area covered by the existing ROI are used (or width=0, height=0)", optional = true, create = true)
@AddJIPipeOutputSlot(value = Roi2dListData.class, name = "ROI", create = true)
@ConfigureJIPipeNode(function = RoiFiltersFunction.class, framework = ImageJ1Framework.class, spatialDimensions = { SPATIAL_2D, SPATIAL_3D })
public class DrawTextRoiAlgorithm extends JIPipeIteratingAlgorithm {

    private final VisualLocationROIProperties roiProperties;
    private JIPipeExpressionParameter text = new JIPipeExpressionParameter("\"your text here\"");

    private FixedMargin location = new FixedMargin();
    private boolean center = false;
    private JIPipeFontFamilySizeStyleParameter font = new JIPipeFontFamilySizeStyleParameter();
    private double angle = 0;

    private boolean antialiased = true;

    private OptionalColorParameter backgroundColor = new OptionalColorParameter(Color.BLACK, false);
    private InnerMargin backgroundMargin = new InnerMargin(2, 2, 2, 2);


    public DrawTextRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualLocationROIProperties();
        registerSubParameter(roiProperties);
    }

    public DrawTextRoiAlgorithm(DrawTextRoiAlgorithm other) {
        super(other);
        this.roiProperties = new VisualLocationROIProperties(other.roiProperties);
        registerSubParameter(roiProperties);
        this.font = new JIPipeFontFamilySizeStyleParameter(other.font);
        this.location = new FixedMargin(other.location);
        this.center = other.center;
        this.angle = other.angle;
        this.antialiased = other.antialiased;
        this.text = new JIPipeExpressionParameter(other.text);
        this.backgroundColor = new OptionalColorParameter(other.backgroundColor);
        this.backgroundMargin = new InnerMargin(other.backgroundMargin);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Generate variables
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        // Collect target and reference
        Roi2dListData target = iterationStep.getInputData("ROI", Roi2dListData.class, progressInfo);
        if (target == null) {
            target = new Roi2dListData();
        } else {
            target = new Roi2dListData(target);
        }
        Rectangle reference;
        ImagePlusData referenceImage = iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo);
        if (referenceImage != null) {
            reference = new Rectangle(0, 0, referenceImage.getWidth(), referenceImage.getHeight());
        } else {
            reference = target.getBounds();
        }

        String finalText = text.evaluateToString(variables);
        Font font2 = font.toFont();

        // Calculate font metrics
        Canvas canvas = new Canvas();
        FontMetrics fontMetrics = canvas.getFontMetrics(font2);
        Rectangle2D stringBounds = fontMetrics.getStringBounds(finalText, canvas.getGraphics());

        // Generate location
        Rectangle finalLocation = location.place(stringBounds.getBounds(), reference, variables);
        if (center) {
            finalLocation.x -= finalLocation.width / 2;
            finalLocation.y -= finalLocation.height / 2;
        }

        // Generate background
        if (backgroundColor.isEnabled()) {
            int left = backgroundMargin.getLeft().evaluateToInteger(variables);
            int top = backgroundMargin.getTop().evaluateToInteger(variables);
            int right = backgroundMargin.getRight().evaluateToInteger(variables);
            int bottom = backgroundMargin.getBottom().evaluateToInteger(variables);
            Roi backgroundRoi = new ShapeRoi(new Rectangle(finalLocation.x - left,
                    finalLocation.y - top,
                    finalLocation.width + left + right,
                    finalLocation.height + top + bottom));
            roiProperties.applyTo(backgroundRoi, variables);
            backgroundRoi.setFillColor(backgroundColor.getContent());
            backgroundRoi.setStrokeColor(backgroundColor.getContent());
            target.add(backgroundRoi);
        }

        // Generate items
        TextRoi textRoi = new TextRoi(finalLocation.x, finalLocation.y, finalLocation.width, finalLocation.height, finalText, font2);
        textRoi.setAngle(angle);
        textRoi.setAntialiased(antialiased);
        roiProperties.applyTo(textRoi, variables);

        target.add(textRoi);

        // Output
        iterationStep.addOutputData(getFirstOutputSlot(), target, progressInfo);
    }


    @SetJIPipeDocumentation(name = "Background color", description = "If enabled, draw a rectangular background ROI behind the text")
    @JIPipeParameter("background-color")
    public OptionalColorParameter getBackgroundColor() {
        return backgroundColor;
    }

    @JIPipeParameter("background-color")
    public void setBackgroundColor(OptionalColorParameter backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @SetJIPipeDocumentation(name = "Background margin", description = "If 'Background color' is enabled, set the distance between the text and the background")
    @JIPipeParameter("background-margin")
    public InnerMargin getBackgroundMargin() {
        return backgroundMargin;
    }

    @JIPipeParameter("background-margin")
    public void setBackgroundMargin(InnerMargin backgroundMargin) {
        this.backgroundMargin = backgroundMargin;
    }

    @SetJIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public VisualLocationROIProperties getRoiProperties() {
        return roiProperties;
    }


    @SetJIPipeDocumentation(name = "Font", description = "The font (family, size and style) of the text")
    @JIPipeParameter("font")
    public JIPipeFontFamilySizeStyleParameter getFont() {
        return font;
    }

    @JIPipeParameter("font")
    public void setFont(JIPipeFontFamilySizeStyleParameter font) {
        this.font = font;
    }

    @SetJIPipeDocumentation(name = "Text", description = "Expression that generates the text")
    @JIPipeParameter(value = "text", important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getText() {
        return text;
    }

    @JIPipeParameter("text")
    public void setText(JIPipeExpressionParameter text) {
        this.text = text;
    }

    @SetJIPipeDocumentation(name = "Location", description = "Determines the location of the text relative to the reference image/bounds of existing ROI")
    @JIPipeParameter("location")
    public FixedMargin getLocation() {
        return location;
    }

    @JIPipeParameter("location")
    public void setLocation(FixedMargin location) {
        this.location = location;
    }

    @SetJIPipeDocumentation(name = "Center at location", description = "If enabled, the calculated (x,y) location will be the center of the text")
    @JIPipeParameter("center")
    public boolean isCenter() {
        return center;
    }

    @JIPipeParameter("center")
    public void setCenter(boolean center) {
        this.center = center;
    }

    @SetJIPipeDocumentation(name = "Angle", description = "Angle of the text")
    @JIPipeParameter("angle")
    public double getAngle() {
        return angle;
    }

    @JIPipeParameter("angle")
    public void setAngle(double angle) {
        this.angle = angle;
    }

    @SetJIPipeDocumentation(name = "Antialiased text", description = "If enabled, text will be antialiased")
    @JIPipeParameter("antialiased")
    public boolean isAntialiased() {
        return antialiased;
    }

    @JIPipeParameter("antialiased")
    public void setAntialiased(boolean antialiased) {
        this.antialiased = antialiased;
    }

    /**
     * #1339: migrate the legacy split font parameters (family/size/style) into
     * the single {@code font} parameter when a pre-migration project is loaded.
     * The text color is the ROI line color (see {@code roi-properties}), not a
     * font-specific field, so it is left at the parameter default. Idempotent:
     * after the next save the legacy keys are gone.
     */
    @Override
    protected void onDeserialized(JsonNode node, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        if (node.has("font-family") || node.has("font-size") || node.has("font-style")) {
            this.font = JIPipeFontFamilySizeStyleParameter.fromLegacyKeys(node, "font-family", "font-size", "font-style", null, 12);
        }
    }
}
