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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import ij.gui.OvalRoi;
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
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
@JIPipeDocumentation(name = "Table to rectangular/oval ROIs", description = "Converts data from a table to rectangular or oval ROIs")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class TableToRectangularROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter columnX1 = new TableColumnSourceExpressionParameter("\"X1\"");
    private TableColumnSourceExpressionParameter columnY1 = new TableColumnSourceExpressionParameter("\"Y1\"");
    private TableColumnSourceExpressionParameter columnX2 = new TableColumnSourceExpressionParameter("\"X2\"");
    private TableColumnSourceExpressionParameter columnY2 = new TableColumnSourceExpressionParameter("\"Y2\"");
    private TableColumnSourceExpressionParameter columnWidth = new TableColumnSourceExpressionParameter("\"Width\"");
    private TableColumnSourceExpressionParameter columnHeight = new TableColumnSourceExpressionParameter("\"Height\"");
    private Anchor anchor = Anchor.TopLeft;
    private Mode mode = Mode.Rectangle;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TableToRectangularROIAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ResultsTableData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
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
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Column 'X1'").report(columnX1);
        report.forCategory("Column 'Y1'").report(columnY1);
        if (anchor == Anchor.TopLeft || anchor == Anchor.Center) {
            report.forCategory("Column 'Width'").report(columnWidth);
            report.forCategory("Column 'Height'").report(columnHeight);
        }
        if (anchor == Anchor.TwoPoints) {
            report.forCategory("Column 'X2'").report(columnX2);
            report.forCategory("Column 'Y2'").report(columnY2);
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ROIListData rois = new ROIListData();

        TableColumn colX1 = columnX1.pickColumn(table);
        TableColumn colY1 = columnY1.pickColumn(table);
        if (colX1 == null) {
            throw new UserFriendlyRuntimeException("Could not find column for X1!",
                    "The algorithm requires a column that provides coordinate X1.",
                    getName() + ", table " + table,
                    "A column reference or generator is required that supplies the coordinates.",
                    "Please check if the settings are correct and if your table contains the requested column.");
        }
        if (colY1 == null) {
            throw new UserFriendlyRuntimeException("Could not find column for Y1!",
                    "The algorithm requires a column that provides coordinate Y1.",
                    getName() + ", table " + table,
                    "A column reference or generator is required that supplies the coordinates.",
                    "Please check if the settings are correct and if your table contains the requested column.");
        }

        if (anchor == Anchor.TopLeft || anchor == Anchor.Center) {

            TableColumn colWidth = columnWidth.pickColumn(table);
            TableColumn colHeight = columnHeight.pickColumn(table);

            if (colWidth == null) {
                throw new UserFriendlyRuntimeException("Could not find column for width!",
                        "The algorithm requires a column that provides the width.",
                        getName() + ", table " + table,
                        "A column reference or generator is required that supplies the width.",
                        "Please check if the settings are correct and if your table contains the requested column.");
            }
            if (colHeight == null) {
                throw new UserFriendlyRuntimeException("Could not find column for height!",
                        "The algorithm requires a column that provides the height.",
                        getName() + ", table " + table,
                        "A column reference or generator is required that supplies the height.",
                        "Please check if the settings are correct and if your table contains the requested column.");
            }

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
                createROI(rois, w, h, x, y);
            }
        } else {
            TableColumn colX2 = columnX2.pickColumn(table);
            TableColumn colY2 = columnY2.pickColumn(table);
            if (colX2 == null) {
                throw new UserFriendlyRuntimeException("Could not find column for X2!",
                        "The algorithm requires a column that provides coordinate X2.",
                        getName() + ", table " + table,
                        "A column reference or generator is required that supplies the coordinates.",
                        "Please check if the settings are correct and if your table contains the requested column.");
            }
            if (colY2 == null) {
                throw new UserFriendlyRuntimeException("Could not find column for Y2!",
                        "The algorithm requires a column that provides coordinate Y2.",
                        getName() + ", table " + table,
                        "A column reference or generator is required that supplies the coordinates.",
                        "Please check if the settings are correct and if your table contains the requested column.");
            }

            for (int row = 0; row < table.getRowCount(); row++) {
                int x1 = (int) colX1.getRowAsDouble(row);
                int y1 = (int) colY1.getRowAsDouble(row);
                int x2 = (int) colX2.getRowAsDouble(row);
                int y2 = (int) colX2.getRowAsDouble(row);
                int w = Math.abs(x1 - x2);
                int h = Math.abs(y1 - y2);
                int x = Math.min(x1, x2);
                int y = Math.min(y1, y2);
                createROI(rois, w, h, x, y);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    private void createROI(ROIListData rois, int w, int h, int x, int y) {
        switch (mode) {
            case Rectangle:
                rois.add(new ShapeRoi(new Rectangle(x, y, w, h)));
                break;
            case Oval:
                rois.add(new OvalRoi(x, y, w, h));
                break;
            case MinCircle: {
                int d = Math.min(w, h);
                int cx = x + w / 2 - d / 2;
                int cy = y + h / 2 - d / 2;
                rois.add(new OvalRoi(cx, cy, d, d));
            }
            break;
            case MaxCircle: {
                int d = Math.max(w, h);
                int cx = x + w / 2 - d / 2;
                int cy = y + h / 2 - d / 2;
                rois.add(new OvalRoi(cx, cy, d, d));
            }
            break;
        }

    }

    @JIPipeDocumentation(name = "Column 'X1'", description = "The table column that is used for the X1 coordinate.")
    @JIPipeParameter(value = "column-x1")
    public TableColumnSourceExpressionParameter getColumnX1() {
        return columnX1;
    }

    @JIPipeParameter(value = "column-x1")
    public void setColumnX1(TableColumnSourceExpressionParameter columnX1) {
        this.columnX1 = columnX1;
    }

    @JIPipeDocumentation(name = "Column 'Y1'", description = "The table column that is used for the Y1 coordinate.")
    @JIPipeParameter(value = "column-y1")
    public TableColumnSourceExpressionParameter getColumnY1() {
        return columnY1;
    }

    @JIPipeParameter(value = "column-y1")
    public void setColumnY1(TableColumnSourceExpressionParameter columnY1) {
        this.columnY1 = columnY1;
    }

    @JIPipeDocumentation(name = "Column 'X2'", description = "The table column that is used for the X2 coordinate. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-x2")
    public TableColumnSourceExpressionParameter getColumnX2() {
        return columnX2;
    }

    @JIPipeParameter(value = "column-x2")
    public void setColumnX2(TableColumnSourceExpressionParameter columnX2) {
        this.columnX2 = columnX2;
    }

    @JIPipeDocumentation(name = "Column 'Y2'", description = "The table column that is used for the Y2 coordinate. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-y2")
    public TableColumnSourceExpressionParameter getColumnY2() {
        return columnY2;
    }

    @JIPipeParameter(value = "column-y2")
    public void setColumnY2(TableColumnSourceExpressionParameter columnY2) {
        this.columnY2 = columnY2;
    }

    @JIPipeDocumentation(name = "Column 'Width'", description = "The table column that is used for the width. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-width")
    public TableColumnSourceExpressionParameter getColumnWidth() {
        return columnWidth;
    }

    @JIPipeParameter(value = "column-width")
    public void setColumnWidth(TableColumnSourceExpressionParameter columnWidth) {
        this.columnWidth = columnWidth;
    }

    @JIPipeDocumentation(name = "Column 'Height'", description = "The table column that is used for the height. " +
            "The usage of this column depends on the current 'Anchor' setting.")
    @JIPipeParameter(value = "column-height")
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
