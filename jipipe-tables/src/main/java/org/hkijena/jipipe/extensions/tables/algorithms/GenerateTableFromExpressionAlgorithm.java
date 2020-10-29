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

package org.hkijena.jipipe.extensions.tables.algorithms;

import com.fathzer.soft.javaluator.StaticVariableSet;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.TableCellExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@JIPipeDocumentation(name = "Table from expressions", description = "Generates a table from expressions")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class GenerateTableFromExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ExpressionTableColumnGeneratorProcessorParameterList columns = new ExpressionTableColumnGeneratorProcessorParameterList();
    private int generatedRows = 10;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public GenerateTableFromExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        columns.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GenerateTableFromExpressionAlgorithm(GenerateTableFromExpressionAlgorithm other) {
        super(other);
        this.generatedRows = other.generatedRows;
        this.columns = new ExpressionTableColumnGeneratorProcessorParameterList(other.columns);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = new ResultsTableData();
        table.addRows(generatedRows);
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        for (ExpressionTableColumnGeneratorProcessor entry : columns) {
            String columnName = entry.getValue();
            int columnId = table.getColumnIndex(columnName);
            if(columnId == -1)
                columnId = table.getOrCreateColumnIndex(columnName);
            variableSet.set("column", columnId);
            variableSet.set("column_name", columnName);
            for (int row = 0; row < table.getRowCount(); row++) {
                variableSet.set("row", row);
                Object value = entry.getKey().evaluate(variableSet);
                if(!(value instanceof Number) && !(value instanceof String))
                    value = "" + value;
                table.setValueAt(value, row, columnId);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), table);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Columns").report(columns);
    }


    @JIPipeDocumentation(name = "Columns", description = "Columns to be generated")
    @JIPipeParameter("columns")
    @ExpressionParameterSettings(variableSource = TableCellExpressionParameterVariableSource.class)
    @PairParameterSettings(singleRow = false, keyLabel = "Function", valueLabel = "Output column")
    public ExpressionTableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ExpressionTableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
    }

    @JIPipeDocumentation(name = "Generated rows", description = "Determines how many rows to generate")
    @JIPipeParameter("generated-rows")
    public int getGeneratedRows() {
        return generatedRows;
    }

    @JIPipeParameter("generated-rows")
    public boolean setGeneratedRows(int generatedRows) {
        if(generatedRows < 0)
            return false;
        this.generatedRows = generatedRows;
        return true;
    }
}
