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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.TableCellExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Table to circular ROIs", description = "Converts data from a table to circular ROIs")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class TableToCircularROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter columnX1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X\"");
    private TableColumnSourceExpressionParameter columnY1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y\"");
    private TableColumnSourceExpressionParameter columnRadius = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Radius\"");

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TableToCircularROIAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", "", ResultsTableData.class)
                .addOutputSlot("Output", "", ROIListData.class, null)
                .seal()
                .build());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableToCircularROIAlgorithm(TableToCircularROIAlgorithm other) {
        super(other);
        this.columnX1 = new TableColumnSourceExpressionParameter(other.columnX1);
        this.columnY1 = new TableColumnSourceExpressionParameter(other.columnY1);
        this.columnRadius = new TableColumnSourceExpressionParameter(other.columnRadius);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ROIListData rois = new ROIListData();

        TableColumn colX1 = columnX1.pickOrGenerateColumn(table);
        TableColumn colY1 = columnY1.pickOrGenerateColumn(table);
        TableColumn colRadius = columnRadius.pickOrGenerateColumn(table);
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
        if (colRadius == null) {
            throw new UserFriendlyRuntimeException("Could not find column for width!",
                    "The algorithm requires a column that provides the width.",
                    getName() + ", table " + table,
                    "A column reference or generator is required that supplies the width.",
                    "Please check if the settings are correct and if your table contains the requested column.");
        }

        for (int row = 0; row < table.getRowCount(); row++) {
            int x1 = (int) colX1.getRowAsDouble(row);
            int y1 = (int) colY1.getRowAsDouble(row);
            int r = (int) colRadius.getRowAsDouble(row);
            int x = x1 - r;
            int y = y1 - r;
            rois.add(new OvalRoi(x, y, r * 2, r * 2));
        }

        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @JIPipeDocumentation(name = "Column 'X1'", description = "The table column that is used for the X1 coordinate. " + TableColumnSourceExpressionParameter.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter(value = "column-x1")
    @ExpressionParameterSettings(variableSource = TableCellExpressionParameterVariableSource.class)
    public TableColumnSourceExpressionParameter getColumnX1() {
        return columnX1;
    }

    @JIPipeParameter(value = "column-x1")
    public void setColumnX1(TableColumnSourceExpressionParameter columnX1) {
        this.columnX1 = columnX1;
    }

    @JIPipeDocumentation(name = "Column 'Y1'", description = "The table column that is used for the Y1 coordinate. " + TableColumnSourceExpressionParameter.DOCUMENTATION_DESCRIPTION)
    @ExpressionParameterSettings(variableSource = TableCellExpressionParameterVariableSource.class)
    @JIPipeParameter(value = "column-y1")
    public TableColumnSourceExpressionParameter getColumnY1() {
        return columnY1;
    }

    @JIPipeParameter(value = "column-y1")
    public void setColumnY1(TableColumnSourceExpressionParameter columnY1) {
        this.columnY1 = columnY1;
    }

    @JIPipeDocumentation(name = "Column 'Radius'", description = "The table column that is used for the radius. " + TableColumnSourceExpressionParameter.DOCUMENTATION_DESCRIPTION)
    @ExpressionParameterSettings(variableSource = TableCellExpressionParameterVariableSource.class)
    @JIPipeParameter(value = "column-radius")
    public TableColumnSourceExpressionParameter getColumnRadius() {
        return columnRadius;
    }

    @JIPipeParameter(value = "column-radius")
    public void setColumnRadius(TableColumnSourceExpressionParameter columnRadius) {
        this.columnRadius = columnRadius;
    }
}
