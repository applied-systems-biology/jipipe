/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.generate;

import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Table to rectangular/oval ROIs", description = "Converts data from a table to rectangular or oval ROIs. This node provides more options than the 'Table to circular ROIs' node.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class TableToRectangularROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter columnX1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X1\"");
    private TableColumnSourceExpressionParameter columnY1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y1\"");
    private TableColumnSourceExpressionParameter columnX2 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X2\"");
    private TableColumnSourceExpressionParameter columnY2 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y2\"");
    private TableColumnSourceExpressionParameter columnWidth = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Width\"");
    private TableColumnSourceExpressionParameter columnHeight = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Height\"");

    private TableColumnSourceExpressionParameter columnZ = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private TableColumnSourceExpressionParameter columnC = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private TableColumnSourceExpressionParameter columnT = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

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
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableToRectangularROIAlgorithm(TableToRectangularROIAlgorithm other) {
        super(other);
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
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        report.resolve("Column 'X1'").report(columnX1);
        report.resolve("Column 'Y1'").report(columnY1);
        if (anchor == Anchor.TopLeft || anchor == Anchor.Center) {
            report.resolve("Column 'Width'").report(columnWidth);
            report.resolve("Column 'Height'").report(columnHeight);
        }
        if (anchor == Anchor.TwoPoints) {
            report.resolve("Column 'X2'").report(columnX2);
            report.resolve("Column 'Y2'").report(columnY2);
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ROIListData rois = new ROIListData();

        TableColumn colX1 = columnX1.pickOrGenerateColumn(table);
        TableColumn colY1 = columnY1.pickOrGenerateColumn(table);
        TableColumn colZ = columnZ.pickOrGenerateColumn(table);
        TableColumn colC = columnC.pickOrGenerateColumn(table);
        TableColumn colT = columnT.pickOrGenerateColumn(table);

        ensureColumnExists(colX1, table, "X1");
        ensureColumnExists(colY1, table, "Y1");
        ensureColumnExists(colZ, table, "Z");
        ensureColumnExists(colC, table, "C");
        ensureColumnExists(colT, table, "T");

        if (anchor == Anchor.TopLeft || anchor == Anchor.Center) {

            TableColumn colWidth = columnWidth.pickOrGenerateColumn(table);
            TableColumn colHeight = columnHeight.pickOrGenerateColumn(table);

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
                createROI(rois, w, h, x, y, z, c, t);
            }
        } else {
            TableColumn colX2 = columnX2.pickOrGenerateColumn(table);
            TableColumn colY2 = columnY2.pickOrGenerateColumn(table);

            ensureColumnExists(colX2, table, "X1");
            ensureColumnExists(colY2, table, "Y1");

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
                createROI(rois, w, h, x, y, z, c, t);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    private void createROI(ROIListData rois, int w, int h, int x, int y, int z, int c, int t) {
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
        rois.add(roi);
    }

    private void ensureColumnExists(TableColumn column, ResultsTableData table, String name) {
        if (column == null) {
            throw new UserFriendlyRuntimeException("Could not find column for " + name + "!",
                    "The algorithm requires a column that provides coordinate " + name + ".",
                    getName() + ", table " + table,
                    "A column reference or generator is required that supplies the coordinates.",
                    "Please check if the settings are correct and if your table contains the requested column.");
        }
    }

    @JIPipeDocumentation(name = "Column 'X1'", description = "The table column that is used for the X1 coordinate.")
    @JIPipeParameter(value = "column-x1", uiOrder = -100)
    public TableColumnSourceExpressionParameter getColumnX1() {
        return columnX1;
    }

    @JIPipeParameter(value = "column-x1")
    public void setColumnX1(TableColumnSourceExpressionParameter columnX1) {
        this.columnX1 = columnX1;
    }

    @JIPipeDocumentation(name = "Column 'Y1'", description = "The table column that is used for the Y1 coordinate.")
    @JIPipeParameter(value = "column-y1", uiOrder = -90)
    public TableColumnSourceExpressionParameter getColumnY1() {
        return columnY1;
    }

    @JIPipeParameter(value = "column-y1")
    public void setColumnY1(TableColumnSourceExpressionParameter columnY1) {
        this.columnY1 = columnY1;
    }

    @JIPipeDocumentation(name = "Column 'X2'", description = "The table column that is used for the X2 coordinate. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-x2", uiOrder = -80)
    public TableColumnSourceExpressionParameter getColumnX2() {
        return columnX2;
    }

    @JIPipeParameter(value = "column-x2")
    public void setColumnX2(TableColumnSourceExpressionParameter columnX2) {
        this.columnX2 = columnX2;
    }

    @JIPipeDocumentation(name = "Column 'Y2'", description = "The table column that is used for the Y2 coordinate. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-y2", uiOrder = -70)
    public TableColumnSourceExpressionParameter getColumnY2() {
        return columnY2;
    }

    @JIPipeParameter(value = "column-y2")
    public void setColumnY2(TableColumnSourceExpressionParameter columnY2) {
        this.columnY2 = columnY2;
    }

    @JIPipeDocumentation(name = "Column 'Width'", description = "The table column that is used for the width. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-width", uiOrder = -60)
    public TableColumnSourceExpressionParameter getColumnWidth() {
        return columnWidth;
    }

    @JIPipeParameter(value = "column-width")
    public void setColumnWidth(TableColumnSourceExpressionParameter columnWidth) {
        this.columnWidth = columnWidth;
    }

    @JIPipeDocumentation(name = "Column 'Height'", description = "The table column that is used for the height. " +
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

    @JIPipeDocumentation(name = "Anchor", description = "Determines how the ROI are generated.\n" +
            "'Top left' creates the ROI at the top left X1 and Y1 coordinates with provided width and height. " +
            "'Center' creates centers the ROI at the X1 and Y1 coordinates and also requires that width and height are set. " +
            "'Two points' defines the ROI, so it fits into the rectangle defined by the points (X1, Y1) and (X2, Y2).")
    @JIPipeParameter(value = "anchor")
    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }

    @JIPipeDocumentation(name = "Created ROI type", description = "Determines which ROI to create. \n" +
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

    @JIPipeDocumentation(name = "Column 'Z'", description = "Table column that determines the Z location. For one-based positions, 0 indicates that the ROI is present in all Z-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-z", uiOrder = -40)
    public TableColumnSourceExpressionParameter getColumnZ() {
        return columnZ;
    }

    @JIPipeParameter("column-z")
    public void setColumnZ(TableColumnSourceExpressionParameter columnZ) {
        this.columnZ = columnZ;
    }

    @JIPipeDocumentation(name = "Column 'C'", description = "Table column that determines the channel location. For one-based positions, 0 indicates that the ROI is present in all channel-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-c", uiOrder = -30)
    public TableColumnSourceExpressionParameter getColumnC() {
        return columnC;
    }

    @JIPipeParameter("column-c")
    public void setColumnC(TableColumnSourceExpressionParameter columnC) {
        this.columnC = columnC;
    }

    @JIPipeDocumentation(name = "Column 'T'", description = "Table column that determines the frame location. For one-based positions, 0 indicates that the ROI is present in all frame-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-t", uiOrder = -20)
    public TableColumnSourceExpressionParameter getColumnT() {
        return columnT;
    }

    @JIPipeParameter("column-t")
    public void setColumnT(TableColumnSourceExpressionParameter columnT) {
        this.columnT = columnT;
    }

    @JIPipeDocumentation(name = "Use one-based positions", description = "If enabled, the first slice is 1. Otherwise, the first slice is zero.")
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
