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
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableCellExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.draw.VisualROIProperties;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Table to 2D circular ROI", description = "Converts data from a table to circular ROIs. The ROIs are created to be centered at the provided locations. If you require more options, utilize 'Table to rectangular/oval ROIs' instead.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeNodeAlias(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Draw", aliasName = "Draw circular ROIs from table")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class TableToCircularROIAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final VisualROIProperties roiProperties;
    private TableColumnSourceExpressionParameter columnX1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"X\"");
    private TableColumnSourceExpressionParameter columnY1 = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Y\"");
    private TableColumnSourceExpressionParameter columnRadius = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Radius\"");
    private TableColumnSourceExpressionParameter columnZ = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");
    private TableColumnSourceExpressionParameter columnC = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");
    private TableColumnSourceExpressionParameter columnT = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "0");
    private boolean oneBasedPositions = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public TableToCircularROIAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.roiProperties = new VisualROIProperties();
        registerSubParameter(roiProperties);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableToCircularROIAlgorithm(TableToCircularROIAlgorithm other) {
        super(other);
        this.roiProperties = new VisualROIProperties(other.roiProperties);
        registerSubParameter(roiProperties);
        this.columnX1 = new TableColumnSourceExpressionParameter(other.columnX1);
        this.columnY1 = new TableColumnSourceExpressionParameter(other.columnY1);
        this.columnRadius = new TableColumnSourceExpressionParameter(other.columnRadius);
        this.columnC = new TableColumnSourceExpressionParameter(other.columnC);
        this.columnZ = new TableColumnSourceExpressionParameter(other.columnZ);
        this.columnT = new TableColumnSourceExpressionParameter(other.columnT);
        this.oneBasedPositions = other.oneBasedPositions;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ROI2DListData rois = new ROI2DListData();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        TableColumnData colX1 = columnX1.pickOrGenerateColumn(table, variables);
        TableColumnData colY1 = columnY1.pickOrGenerateColumn(table, variables);
        TableColumnData colRadius = columnRadius.pickOrGenerateColumn(table, variables);
        TableColumnData colZ = columnZ.pickOrGenerateColumn(table, variables);
        TableColumnData colC = columnC.pickOrGenerateColumn(table, variables);
        TableColumnData colT = columnT.pickOrGenerateColumn(table, variables);

        ensureColumnExists(colX1, table, "X1");
        ensureColumnExists(colY1, table, "Y1");
        ensureColumnExists(colRadius, table, "Radius");
        ensureColumnExists(colZ, table, "Z");
        ensureColumnExists(colC, table, "C");
        ensureColumnExists(colT, table, "T");

        for (int row = 0; row < table.getRowCount(); row++) {
            int x1 = (int) colX1.getRowAsDouble(row);
            int y1 = (int) colY1.getRowAsDouble(row);
            int r = (int) colRadius.getRowAsDouble(row);
            int x = x1 - r;
            int y = y1 - r;
            int z = (int) colZ.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            int c = (int) colC.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            int t = (int) colT.getRowAsDouble(row) + (oneBasedPositions ? 0 : 1);
            Roi roi = new OvalRoi(x, y, r * 2, r * 2);
            roi.setPosition(c, z, t);
            roiProperties.applyTo(roi, variables);
            rois.add(roi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
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

    @SetJIPipeDocumentation(name = "ROI properties", description = "Use the following settings to customize the generated ROI")
    @JIPipeParameter("roi-properties")
    public VisualROIProperties getRoiProperties() {
        return roiProperties;
    }

    @SetJIPipeDocumentation(name = "Column 'X1'", description = "The table column that is used for the X1 coordinate. ")
    @JIPipeParameter(value = "column-x1", uiOrder = -100)
    @JIPipeExpressionParameterSettings(variableSource = TableCellExpressionParameterVariablesInfo.class)
    public TableColumnSourceExpressionParameter getColumnX1() {
        return columnX1;
    }

    @JIPipeParameter(value = "column-x1")
    public void setColumnX1(TableColumnSourceExpressionParameter columnX1) {
        this.columnX1 = columnX1;
    }

    @SetJIPipeDocumentation(name = "Column 'Y1'", description = "The table column that is used for the Y1 coordinate. ")
    @JIPipeExpressionParameterSettings(variableSource = TableCellExpressionParameterVariablesInfo.class)
    @JIPipeParameter(value = "column-y1", uiOrder = -90)
    public TableColumnSourceExpressionParameter getColumnY1() {
        return columnY1;
    }

    @JIPipeParameter(value = "column-y1")
    public void setColumnY1(TableColumnSourceExpressionParameter columnY1) {
        this.columnY1 = columnY1;
    }

    @SetJIPipeDocumentation(name = "Column 'Radius'", description = "The table column that is used for the radius. ")
    @JIPipeExpressionParameterSettings(variableSource = TableCellExpressionParameterVariablesInfo.class)
    @JIPipeParameter(value = "column-radius", uiOrder = -80)
    public TableColumnSourceExpressionParameter getColumnRadius() {
        return columnRadius;
    }

    @JIPipeParameter(value = "column-radius")
    public void setColumnRadius(TableColumnSourceExpressionParameter columnRadius) {
        this.columnRadius = columnRadius;
    }

    @SetJIPipeDocumentation(name = "Column 'Z'", description = "Table column that determines the Z location. For one-based positions, 0 indicates that the ROI is present in all Z-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-z", uiOrder = -70)
    public TableColumnSourceExpressionParameter getColumnZ() {
        return columnZ;
    }

    @JIPipeParameter("column-z")
    public void setColumnZ(TableColumnSourceExpressionParameter columnZ) {
        this.columnZ = columnZ;
    }

    @SetJIPipeDocumentation(name = "Column 'C'", description = "Table column that determines the channel location. For one-based positions, 0 indicates that the ROI is present in all channel-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-c", uiOrder = -60)
    public TableColumnSourceExpressionParameter getColumnC() {
        return columnC;
    }

    @JIPipeParameter("column-c")
    public void setColumnC(TableColumnSourceExpressionParameter columnC) {
        this.columnC = columnC;
    }

    @SetJIPipeDocumentation(name = "Column 'T'", description = "Table column that determines the frame location. For one-based positions, 0 indicates that the ROI is present in all frame-slices. For zero-based positions the value is -1 or lower.")
    @JIPipeParameter(value = "column-t", uiOrder = -50)
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
