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

import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
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
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.draw.VisualROIProperties;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Table to 2D rectangular/oval ROI", description = "Converts data from a table to rectangular or oval ROIs. This node provides more options than the 'Table to circular ROIs' node.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeNodeAlias(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw", aliasName = "Draw rectangular ROIs from table")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class TableToRectangularROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VisualROIProperties roiProperties;
    private TableColumnSourceExpressionParameter columnX1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X1\"");
    private TableColumnSourceExpressionParameter columnY1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y1\"");
    private TableColumnSourceExpressionParameter columnX2 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X2\"");
    private TableColumnSourceExpressionParameter columnY2 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y2\"");
    private TableColumnSourceExpressionParameter columnWidth = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Width\"");
    private TableColumnSourceExpressionParameter columnHeight = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Height\"");
    private TableColumnSourceExpressionParameter columnZ = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");
    private TableColumnSourceExpressionParameter columnC = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");
    private TableColumnSourceExpressionParameter columnT = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");
    private TableColumnSourceExpressionParameter columnName = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "\"ROI name\"");
    private TableColumnSourceExpressionParameter columnMetadata = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "TO_JSON(MAP())");
    private boolean oneBasedPositions = true;
    private Anchor anchor = Anchor.TopLeft;
    private Mode mode = Mode.Rectangle;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TableToRectangularROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualROIProperties();
        this.roiProperties.getRoiName().setEnabled(false);
        registerSubParameter(roiProperties);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableToRectangularROIAlgorithm(TableToRectangularROIAlgorithm other) {
        super(other);
        this.roiProperties = new VisualROIProperties(other.roiProperties);
        registerSubParameter(roiProperties);
        this.anchor = other.anchor;
        this.mode = other.mode;
        this.columnX1 = new TableColumnSourceExpressionParameter(other.columnX1);
        this.columnY1 = new TableColumnSourceExpressionParameter(other.columnY1);
        this.columnX2 = new TableColumnSourceExpressionParameter(other.columnX2);
        this.columnY2 = new TableColumnSourceExpressionParameter(other.columnY2);
        this.columnWidth = new TableColumnSourceExpressionParameter(other.columnWidth);
        this.columnHeight = new TableColumnSourceExpressionParameter(other.columnHeight);
        this.columnC = new TableColumnSourceExpressionParameter(other.columnC);
        this.columnZ = new TableColumnSourceExpressionParameter(other.columnZ);
        this.columnT = new TableColumnSourceExpressionParameter(other.columnT);
        this.oneBasedPositions = other.oneBasedPositions;
        this.columnName = new TableColumnSourceExpressionParameter(other.columnName);
        this.columnMetadata = new TableColumnSourceExpressionParameter(other.columnMetadata);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Column 'X1'", "column-x1"), columnX1);
        report.report(new ParameterValidationReportContext(reportContext, this, "Column 'Y1'", "column-y1"), columnY1);

        if (anchor == Anchor.TopLeft || anchor == Anchor.Center) {
            report.report(new ParameterValidationReportContext(reportContext, this, "Column 'Width'", "column-width"), columnWidth);
            report.report(new ParameterValidationReportContext(reportContext, this, "Column 'Height'", "column-height"), columnHeight);
        }
        if (anchor == Anchor.TwoPoints) {
            report.report(new ParameterValidationReportContext(reportContext, this, "Column 'X2'", "column-x2"), columnX2);
            report.report(new ParameterValidationReportContext(reportContext, this, "Column 'Y2'", "column-y2"), columnY2);
        }
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ROI2DListData rois = new ROI2DListData();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        TableColumnData colX1 = columnX1.pickOrGenerateColumn(table, variables);
        TableColumnData colY1 = columnY1.pickOrGenerateColumn(table, variables);
        TableColumnData colZ = columnZ.pickOrGenerateColumn(table, variables);
        TableColumnData colC = columnC.pickOrGenerateColumn(table, variables);
        TableColumnData colT = columnT.pickOrGenerateColumn(table, variables);
        TableColumnData colName = columnName.pickOrGenerateColumn(table, variables);
        TableColumnData colMetadata = columnMetadata.pickOrGenerateColumn(table, variables);

        ensureColumnExists(colX1, table, "X1");
        ensureColumnExists(colY1, table, "Y1");
        ensureColumnExists(colZ, table, "Z");
        ensureColumnExists(colC, table, "C");
        ensureColumnExists(colT, table, "T");
        ensureColumnExists(colName, table, "Name");
        ensureColumnExists(colMetadata, table, "Metadata");

        if (anchor == Anchor.TopLeft || anchor == Anchor.Center) {

            TableColumnData colWidth = columnWidth.pickOrGenerateColumn(table, variables);
            TableColumnData colHeight = columnHeight.pickOrGenerateColumn(table, variables);

            ensureColumnExists(colWidth, table, "Width");
            ensureColumnExists(colHeight, table, "Height");

            for (int row = 0; row < table.getRowCount(); row++) {
                int x1 = (int) colX1.getRowAsDouble(row);
                int y1 = (int) colY1.getRowAsDouble(row);
                int w = (int) colWidth.getRowAsDouble(row);
                int h = (int) colHeight.getRowAsDouble(row);
                int x;
                int y;
                if (anchor == Anchor.TopLeft) {
                    x = x1;
                    y = y1;
                } else {
                    x = x1 - w / 2;
                    y = y1 - h / 2;
                }
                int z = (int) colZ.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
                int c = (int) colC.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
                int t = (int) colT.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
                String name = colName.getRowAsString(row);
                String metadata = colMetadata.getRowAsString(row);
                createROI(rois, w, h, x, y, z, c, t, name, metadata, variables);
            }
        } else {
            TableColumnData colX2 = columnX2.pickOrGenerateColumn(table, variables);
            TableColumnData colY2 = columnY2.pickOrGenerateColumn(table, variables);

            ensureColumnExists(colX2, table, "X2");
            ensureColumnExists(colY2, table, "Y2");

            for (int row = 0; row < table.getRowCount(); row++) {
                int x1 = (int) colX1.getRowAsDouble(row);
                int y1 = (int) colY1.getRowAsDouble(row);
                int x2 = (int) colX2.getRowAsDouble(row);
                int y2 = (int) colY2.getRowAsDouble(row);
                int w = Math.abs(x1 - x2);
                int h = Math.abs(y1 - y2);
                int x = Math.min(x1, x2);
                int y = Math.min(y1, y2);
                int z = (int) colZ.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
                int c = (int) colC.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
                int t = (int) colT.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
                String name = colName.getRowAsString(row);
                String metadata = colMetadata.getRowAsString(row);
                createROI(rois, w, h, x, y, z, c, t, name, metadata, variables);
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    private void createROI(ROI2DListData rois, int w, int h, int x, int y, int z, int c, int t, String name, String metadata, JIPipeExpressionVariablesMap variables) {
        Roi roi;
        switch (mode) {
            case Rectangle:
                roi = new ShapeRoi(new Rectangle(x, y, w, h));
                break;
            case Oval:
                roi = new OvalRoi(x, y, w, h);
                break;
            case MinCircle: {
                int d = Math.min(w, h);
                int cx = x + w / 2 - d / 2;
                int cy = y + h / 2 - d / 2;
                roi = new OvalRoi(cx, cy, d, d);
            }
            break;
            case MaxCircle: {
                int d = Math.max(w, h);
                int cx = x + w / 2 - d / 2;
                int cy = y + h / 2 - d / 2;
                roi = new OvalRoi(cx, cy, d, d);
            }
            break;
            default:
                throw new UnsupportedOperationException();
        }
        roi.setPosition(c, z, t);
        roi.setName(StringUtils.orElse(name, "Unnamed"));
        ImageJUtils.setRoiPropertiesFromString(roi, metadata, "metadata_");
        roiProperties.applyTo(roi, variables);
        rois.add(roi);
    }

    private void ensureColumnExists(TableColumnData column, ResultsTableData table, String name) {
        if (column == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Could not find column for " + name + "!",
                    "The algorithm requires a column that provides coordinate " + name + ".",
                    "Please check if the settings are correct and if your table contains the requested column."));
        }
    }

    @SetJIPipeDocumentation(name = "ROI name", description = "The table column that is used for the ROI name (overwritten if the ROI name in 'ROI properties' is enabled)")
    @JIPipeParameter("column-name")
    public TableColumnSourceExpressionParameter getColumnName() {
        return columnName;
    }

    @JIPipeParameter("column-name")
    public void setColumnName(TableColumnSourceExpressionParameter columnName) {
        this.columnName = columnName;
    }

    @SetJIPipeDocumentation(name = "ROI metadata", description = "The table column that contains ROI metadata in as JSON object (string keys, string values). If not valid JSON, the metadata will be stored into a field 'metadata_'.")
    @JIPipeParameter("column-metadata")
    public TableColumnSourceExpressionParameter getColumnMetadata() {
        return columnMetadata;
    }

    @JIPipeParameter("column-metadata")
    public void setColumnMetadata(TableColumnSourceExpressionParameter columnMetadata) {
        this.columnMetadata = columnMetadata;
    }

    @SetJIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public VisualROIProperties getRoiProperties() {
        return roiProperties;
    }

    @SetJIPipeDocumentation(name = "Column 'X1'", description = "The table column that is used for the X1 coordinate.")
    @JIPipeParameter(value = "column-x1", uiOrder = -100)
    public TableColumnSourceExpressionParameter getColumnX1() {
        return columnX1;
    }

    @JIPipeParameter(value = "column-x1")
    public void setColumnX1(TableColumnSourceExpressionParameter columnX1) {
        this.columnX1 = columnX1;
    }

    @SetJIPipeDocumentation(name = "Column 'Y1'", description = "The table column that is used for the Y1 coordinate.")
    @JIPipeParameter(value = "column-y1", uiOrder = -90)
    public TableColumnSourceExpressionParameter getColumnY1() {
        return columnY1;
    }

    @JIPipeParameter(value = "column-y1")
    public void setColumnY1(TableColumnSourceExpressionParameter columnY1) {
        this.columnY1 = columnY1;
    }

    @SetJIPipeDocumentation(name = "Column 'X2'", description = "The table column that is used for the X2 coordinate. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-x2", uiOrder = -80)
    public TableColumnSourceExpressionParameter getColumnX2() {
        return columnX2;
    }

    @JIPipeParameter(value = "column-x2")
    public void setColumnX2(TableColumnSourceExpressionParameter columnX2) {
        this.columnX2 = columnX2;
    }

    @SetJIPipeDocumentation(name = "Column 'Y2'", description = "The table column that is used for the Y2 coordinate. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-y2", uiOrder = -70)
    public TableColumnSourceExpressionParameter getColumnY2() {
        return columnY2;
    }

    @JIPipeParameter(value = "column-y2")
    public void setColumnY2(TableColumnSourceExpressionParameter columnY2) {
        this.columnY2 = columnY2;
    }

    @SetJIPipeDocumentation(name = "Column 'Width'", description = "The table column that is used for the width. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-width", uiOrder = -60)
    public TableColumnSourceExpressionParameter getColumnWidth() {
        return columnWidth;
    }

    @JIPipeParameter(value = "column-width")
    public void setColumnWidth(TableColumnSourceExpressionParameter columnWidth) {
        this.columnWidth = columnWidth;
    }

    @SetJIPipeDocumentation(name = "Column 'Height'", description = "The table column that is used for the height. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-height", uiOrder = -50)
    public TableColumnSourceExpressionParameter getColumnHeight() {
        return columnHeight;
    }

    @JIPipeParameter(value = "column-height")
    public void setColumnHeight(TableColumnSourceExpressionParameter columnHeight) {
        this.columnHeight = columnHeight;
    }

    @JIPipeParameter(value = "anchor")
    public Anchor getAnchor() {
        return anchor;
    }

    @SetJIPipeDocumentation(name = "Anchor", description = "Determines how the ROI are generated.\n" +
            "'Top left' creates the ROI at the top left X1 and Y1 coordinates with provided width and height. " +
            "'Center' creates centers the ROI at the X1 and Y1 coordinates and also requires that width and height are set. " +
            "'Two points' defines the ROI, so it fits into the rectangle defined by the points (X1, Y1) and (X2, Y2).")
    @JIPipeParameter(value = "anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    @SetJIPipeDocumentation(name = "Created ROI type", description = "Determines which ROI to create. \n" +
            "'Rectangle' creates rectangles. 'Oval' creates ovals that fit into the space defined by the rectangle. " +
            "'MinCircle' and 'MaxCircle' creates circles that either fit into the space or encompass it. " +
            "Circles are centered within the rectangle area.")
    @JIPipeParameter("mode")
    public Mode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
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

    /**
     * Available anchors
     */
    public enum Anchor {
        TopLeft,
        TwoPoints,
        Center;


        @Override
        public String toString() {
            switch (this) {
                case Center:
                    return "Center at X1, Y1";
                case TopLeft:
                    return "Top left at X1, Y1";
                case TwoPoints:
                    return "Two points (X1, X2), (Y1, Y2)";
                default:
                    return super.toString();
            }
        }
    }

    public enum Mode {
        Rectangle,
        Oval,
        MinCircle,
        MaxCircle
    }
}
