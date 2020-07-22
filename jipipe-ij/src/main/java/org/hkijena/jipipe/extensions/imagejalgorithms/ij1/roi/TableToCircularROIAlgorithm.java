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
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.TableColumnSourceParameter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Table to circular ROIs", description = "Converts data from a table to circular ROIs")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input")
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output")
public class TableToCircularROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceParameter columnX1 = new TableColumnSourceParameter();
    private TableColumnSourceParameter columnY1 = new TableColumnSourceParameter();
    private TableColumnSourceParameter columnRadius = new TableColumnSourceParameter();

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TableToCircularROIAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .addInputSlot("Input", ResultsTableData.class)
                .addOutputSlot("Output", ROIListData.class, null)
                .seal()
                .build());
        columnX1.setColumnSource(new StringPredicate(StringPredicate.Mode.Equals, "X", false));
        columnY1.setColumnSource(new StringPredicate(StringPredicate.Mode.Equals, "Y", false));
        columnRadius.setColumnSource(new StringPredicate(StringPredicate.Mode.Equals, "Radius", false));
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableToCircularROIAlgorithm(TableToCircularROIAlgorithm other) {
        super(other);
        this.columnX1 = new TableColumnSourceParameter(other.columnX1);
        this.columnY1 = new TableColumnSourceParameter(other.columnY1);
        this.columnRadius = new TableColumnSourceParameter(other.columnRadius);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Column 'X1'").report(columnX1);
        report.forCategory("Column 'Y1'").report(columnY1);
        report.forCategory("Column 'Width'").report(columnRadius);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class);
        ROIListData rois = new ROIListData();

        TableColumn colX1 = columnX1.pickColumn(table);
        TableColumn colY1 = columnY1.pickColumn(table);
        TableColumn colRadius = columnRadius.pickColumn(table);
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

        dataBatch.addOutputData(getFirstOutputSlot(), rois);
    }

    @JIPipeDocumentation(name = "Column 'X1'", description = "The table column that is used for the X1 coordinate.")
    @JIPipeParameter(value = "column-x1")
    public TableColumnSourceParameter getColumnX1() {
        return columnX1;
    }

    @JIPipeParameter(value = "column-x1")
    public void setColumnX1(TableColumnSourceParameter columnX1) {
        this.columnX1 = columnX1;
    }

    @JIPipeDocumentation(name = "Column 'Y1'", description = "The table column that is used for the Y1 coordinate.")
    @JIPipeParameter(value = "column-y1")
    public TableColumnSourceParameter getColumnY1() {
        return columnY1;
    }

    @JIPipeParameter(value = "column-y1")
    public void setColumnY1(TableColumnSourceParameter columnY1) {
        this.columnY1 = columnY1;
    }

    @JIPipeDocumentation(name = "Column 'Radius'", description = "The table column that is used for the radius. ")
    @JIPipeParameter(value = "column-radius")
    public TableColumnSourceParameter getColumnRadius() {
        return columnRadius;
    }

    @JIPipeParameter(value = "column-radius")
    public void setColumnRadius(TableColumnSourceParameter columnRadius) {
        this.columnRadius = columnRadius;
    }
}
