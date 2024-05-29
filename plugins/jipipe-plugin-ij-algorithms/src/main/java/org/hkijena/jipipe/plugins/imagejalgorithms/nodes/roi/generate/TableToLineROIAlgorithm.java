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

import ij.gui.Line;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

import java.awt.*;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Table to line ROIs", description = "Converts data from a table to line ROIs.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeNodeAlias(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw", aliasName = "Draw line ROIs from table")
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class TableToLineROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter columnX1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X1\"");
    private TableColumnSourceExpressionParameter columnY1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y1\"");
    private TableColumnSourceExpressionParameter columnX2 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X2\"");
    private TableColumnSourceExpressionParameter columnY2 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y2\"");

    private TableColumnSourceExpressionParameter columnZ = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private TableColumnSourceExpressionParameter columnC = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private TableColumnSourceExpressionParameter columnT = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");

    private boolean oneBasedPositions = true;

    private final VisualROIProperties roiProperties;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TableToLineROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualROIProperties();
        registerSubParameter(roiProperties);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableToLineROIAlgorithm(TableToLineROIAlgorithm other) {
        super(other);
        this.roiProperties = new VisualROIProperties(other.roiProperties);
        registerSubParameter(roiProperties);
        this.columnX1 = new TableColumnSourceExpressionParameter(other.columnX1);
        this.columnY1 = new TableColumnSourceExpressionParameter(other.columnY1);
        this.columnX2 = new TableColumnSourceExpressionParameter(other.columnX2);
        this.columnY2 = new TableColumnSourceExpressionParameter(other.columnY2);
        this.columnC = new TableColumnSourceExpressionParameter(other.columnC);
        this.columnZ = new TableColumnSourceExpressionParameter(other.columnZ);
        this.columnT = new TableColumnSourceExpressionParameter(other.columnT);
        this.oneBasedPositions = other.oneBasedPositions;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Column 'X1'", "column-x1"), columnX1);
        report.report(new ParameterValidationReportContext(reportContext, this, "Column 'Y1'", "column-y1"), columnY1);
        report.report(new ParameterValidationReportContext(reportContext, this, "Column 'X2'", "column-x2"), columnX2);
        report.report(new ParameterValidationReportContext(reportContext, this, "Column 'Y2'", "column-y2"), columnY2);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ROIListData rois = new ROIListData();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        TableColumn colX1 = columnX1.pickOrGenerateColumn(table, variables);
        TableColumn colY1 = columnY1.pickOrGenerateColumn(table, variables);
        TableColumn colZ = columnZ.pickOrGenerateColumn(table, variables);
        TableColumn colC = columnC.pickOrGenerateColumn(table, variables);
        TableColumn colT = columnT.pickOrGenerateColumn(table, variables);

        ensureColumnExists(colX1, table, "X1");
        ensureColumnExists(colY1, table, "Y1");
        ensureColumnExists(colZ, table, "Z");
        ensureColumnExists(colC, table, "C");
        ensureColumnExists(colT, table, "T");

        TableColumn colX2 = columnX2.pickOrGenerateColumn(table, variables);
        TableColumn colY2 = columnY2.pickOrGenerateColumn(table, variables);

        ensureColumnExists(colX2, table, "X2");
        ensureColumnExists(colY2, table, "Y2");

        for (int row = 0; row < table.getRowCount(); row++) {
            double x1 = (int) colX1.getRowAsDouble(row);
            double y1 = (int) colY1.getRowAsDouble(row);
            double x2 = (int) colX2.getRowAsDouble(row);
            double y2 = (int) colY2.getRowAsDouble(row);
            int z = (int) colZ.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            int c = (int) colC.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            int t = (int) colT.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            createROI(rois, x1, y1, x2, y2, z, c, t, variables);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    private void createROI(ROIListData rois, double x1, double y1, double x2, double y2, int z, int c, int t, JIPipeExpressionVariablesMap variables) {
        Roi roi = new Line(x1, y1, x2, y2);
        roi.setPosition(c, z, t);
        roiProperties.applyTo(roi, variables);
        rois.add(roi);
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
}
