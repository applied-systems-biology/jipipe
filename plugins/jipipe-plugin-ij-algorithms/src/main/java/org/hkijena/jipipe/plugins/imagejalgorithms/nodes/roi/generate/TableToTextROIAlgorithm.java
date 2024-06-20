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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.generate;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.draw.VisualROIProperties;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.FontStyleParameter;
import org.hkijena.jipipe.plugins.parameters.library.roi.InnerMargin;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Table to text ROIs", description = "Converts data from a table to text ROIs.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeNodeAlias(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw", aliasName = "Draw text ROIs from table")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, name = "Output", create = true)
public class TableToTextROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter columnX = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X\"");
    private TableColumnSourceExpressionParameter columnY = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y\"");
    private TableColumnSourceExpressionParameter columnText = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Text\"");

    private TableColumnSourceExpressionParameter columnZ = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private TableColumnSourceExpressionParameter columnC = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private TableColumnSourceExpressionParameter columnT = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private boolean oneBasedPositions = true;
    private boolean centerX = false;
    private boolean centerY = false;
    private FontFamilyParameter fontFamily = new FontFamilyParameter();

    private FontStyleParameter fontStyle = FontStyleParameter.Plain;
    private int fontSize = 12;
    private double angle = 0;

    private boolean antialiased = true;

    private final VisualROIProperties roiProperties;
    private OptionalColorParameter backgroundColor = new OptionalColorParameter(Color.BLACK, false);
    private InnerMargin backgroundMargin = new InnerMargin(2,2,2,2);

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TableToTextROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualROIProperties();
        registerSubParameter(roiProperties);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableToTextROIAlgorithm(TableToTextROIAlgorithm other) {
        super(other);
        this.roiProperties = new VisualROIProperties(other.roiProperties);
        registerSubParameter(roiProperties);
        this.columnX = new TableColumnSourceExpressionParameter(other.columnX);
        this.columnY = new TableColumnSourceExpressionParameter(other.columnY);
        this.columnText = new TableColumnSourceExpressionParameter(other.columnText);
        this.columnC = new TableColumnSourceExpressionParameter(other.columnC);
        this.columnZ = new TableColumnSourceExpressionParameter(other.columnZ);
        this.columnT = new TableColumnSourceExpressionParameter(other.columnT);
        this.oneBasedPositions = other.oneBasedPositions;
        this.fontFamily = new FontFamilyParameter(other.fontFamily);
        this.fontSize = other.fontSize;
        this.fontStyle = other.fontStyle;
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.angle = other.angle;
        this.antialiased = other.antialiased;
        this.backgroundColor = new OptionalColorParameter(other.backgroundColor);
        this.backgroundMargin = new InnerMargin(other.backgroundMargin);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ROIListData rois = new ROIListData();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        TableColumn colX = columnX.pickOrGenerateColumn(table, variables);
        TableColumn colY = columnY.pickOrGenerateColumn(table, variables);
        TableColumn colZ = columnZ.pickOrGenerateColumn(table, variables);
        TableColumn colC = columnC.pickOrGenerateColumn(table, variables);
        TableColumn colT = columnT.pickOrGenerateColumn(table, variables);
        TableColumn colText = columnText.pickOrGenerateColumn(table, variables);

        ensureColumnExists(colX, table, "X1");
        ensureColumnExists(colY, table, "Y1");
        ensureColumnExists(colZ, table, "Z");
        ensureColumnExists(colC, table, "C");
        ensureColumnExists(colT, table, "T");
        ensureColumnExists(colText, table, "Text");

        Font font = fontStyle.toFont(fontFamily, fontSize);

        // Calculate font metrics
        Canvas canvas = new Canvas();
        FontMetrics fontMetrics = canvas.getFontMetrics(font);

        for (int row = 0; row < table.getRowCount(); row++) {
            double x = (int) colX.getRowAsDouble(row);
            double y = (int) colY.getRowAsDouble(row);
            int z = (int) colZ.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            int c = (int) colC.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            int t = (int) colT.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            String text = colText.getRowAsString(row);

            Rectangle2D stringBounds = fontMetrics.getStringBounds(text, canvas.getGraphics());
            double width = stringBounds.getWidth();
            double height = stringBounds.getHeight();

            if(centerX) {
                x -= width / 2.0;
            }
            if(centerY) {
                y -= height / 2.0;
            }

            // Generate background
            if(backgroundColor.isEnabled()) {
                int left = backgroundMargin.getLeft().evaluateToInteger(variables);
                int top = backgroundMargin.getTop().evaluateToInteger(variables);
                int right = backgroundMargin.getRight().evaluateToInteger(variables);
                int bottom = backgroundMargin.getBottom().evaluateToInteger(variables);
                Roi backgroundRoi = new ShapeRoi(new Rectangle2D.Double(x - left,
                        y - top,
                        width + left + right,
                        height + top + bottom));
                roiProperties.applyTo(backgroundRoi, variables);
                backgroundRoi.setFillColor(backgroundColor.getContent());
                backgroundRoi.setStrokeColor(backgroundColor.getContent());
                rois.add(backgroundRoi);
            }

            TextRoi textRoi = new TextRoi(x, y, width, height, text, font);
            textRoi.setAngle(angle);
            textRoi.setAntialiased(antialiased);
            textRoi.setPosition(c, z, t);
            roiProperties.applyTo(textRoi, variables);

            rois.add(textRoi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    private void ensureColumnExists(TableColumn column, ResultsTableData table, String name) {
        if (column == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Could not find column for " + name + "!",
                    "The algorithm requires a column that provides coordinate " + name + ".",
                    "Please check if the settings are correct and if your table contains the requested column."));
        }
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
    public VisualROIProperties getRoiProperties() {
        return roiProperties;
    }

    @SetJIPipeDocumentation(name = "Column 'X'", description = "The table column that is used for the X coordinate.")
    @JIPipeParameter(value = "column-x", uiOrder = -100)
    public TableColumnSourceExpressionParameter getColumnX() {
        return columnX;
    }

    @JIPipeParameter(value = "column-x")
    public void setColumnX(TableColumnSourceExpressionParameter columnX) {
        this.columnX = columnX;
    }

    @SetJIPipeDocumentation(name = "Column 'Y'", description = "The table column that is used for the Y coordinate.")
    @JIPipeParameter(value = "column-y", uiOrder = -90)
    public TableColumnSourceExpressionParameter getColumnY() {
        return columnY;
    }

    @JIPipeParameter(value = "column-y")
    public void setColumnY(TableColumnSourceExpressionParameter columnY) {
        this.columnY = columnY;
    }

    @SetJIPipeDocumentation(name = "Column 'Text'", description = "The table column that is used for the text. ")
    @JIPipeParameter(value = "column-text", uiOrder = -50)
    public TableColumnSourceExpressionParameter getColumnText() {
        return columnText;
    }

    @JIPipeParameter(value = "column-text")
    public void setColumnText(TableColumnSourceExpressionParameter columnText) {
        this.columnText = columnText;
    }

    @SetJIPipeDocumentation(name = "Column 'Z'", description = "Table column that determines the Z location. For one-based positions, 0 indicates that the ROI is present in all Z-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-z", uiOrder = -40)
    public TableColumnSourceExpressionParameter getColumnZ() {
        return columnZ;
    }

    @JIPipeParameter("column-z")
    public void setColumnZ(TableColumnSourceExpressionParameter columnZ) {
        this.columnZ = columnZ;
    }

    @SetJIPipeDocumentation(name = "Column 'C'", description = "Table column that determines the channel location. For one-based positions, 0 indicates that the ROI is present in all channel-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-c", uiOrder = -30)
    public TableColumnSourceExpressionParameter getColumnC() {
        return columnC;
    }

    @JIPipeParameter("column-c")
    public void setColumnC(TableColumnSourceExpressionParameter columnC) {
        this.columnC = columnC;
    }

    @SetJIPipeDocumentation(name = "Column 'T'", description = "Table column that determines the frame location. For one-based positions, 0 indicates that the ROI is present in all frame-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-t", uiOrder = -20)
    public TableColumnSourceExpressionParameter getColumnT() {
        return columnT;
    }

    @JIPipeParameter("column-t")
    public void setColumnT(TableColumnSourceExpressionParameter columnT) {
        this.columnT = columnT;
    }

    @SetJIPipeDocumentation(name = "Use one-based positions", description = "If enabled, the first slice is 1. Otherwise, the first slice is zero.")
    @JIPipeParameter(value = "one-based-positions", important = true)
    public boolean isOneBasedPositions() {
        return oneBasedPositions;
    }

    @JIPipeParameter("one-based-positions")
    public void setOneBasedPositions(boolean oneBasedPositions) {
        this.oneBasedPositions = oneBasedPositions;
    }

    @SetJIPipeDocumentation(name = "Font size", description = "The size of the text")
    @JIPipeParameter("font-size")
    public int getFontSize() {
        return fontSize;
    }

    @JIPipeParameter("font-size")
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    @SetJIPipeDocumentation(name = "Font style", description = "The style of the text")
    @JIPipeParameter("font-style")
    public FontStyleParameter getFontStyle() {
        return fontStyle;
    }

    @JIPipeParameter("font-style")
    public void setFontStyle(FontStyleParameter fontStyle) {
        this.fontStyle = fontStyle;
    }

    @SetJIPipeDocumentation(name = "Center X at location", description = "If enabled, the calculated x location will be the center of the text")
    @JIPipeParameter("center-x")
    public boolean isCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(boolean centerX) {
        this.centerX = centerX;
    }

    @SetJIPipeDocumentation(name = "Center Y at location", description = "If enabled, the calculated y location will be the center of the text")
    @JIPipeParameter("center-y")
    public boolean isCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(boolean centerY) {
        this.centerY = centerY;
    }

    @SetJIPipeDocumentation(name = "Font family", description = "The font of the text")
    @JIPipeParameter("font-family")
    public FontFamilyParameter getFontFamily() {
        return fontFamily;
    }

    @JIPipeParameter("font-family")
    public void setFontFamily(FontFamilyParameter fontFamily) {
        this.fontFamily = fontFamily;
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

}
