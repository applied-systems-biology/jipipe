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
 *
 */

package org.hkijena.jipipe.extensions.tables.nodes.rows;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

import java.util.*;

@JIPipeDocumentation(name = "Apply expression per row", description = "Applies an expression for each row. The column values are available as variables.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ApplyExpressionPerRowAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ExpressionTableColumnGeneratorProcessorParameterList expressionList = new ExpressionTableColumnGeneratorProcessorParameterList();

    public ApplyExpressionPerRowAlgorithm(JIPipeNodeInfo info) {
        super(info);
        expressionList.addNewInstance();
    }

    public ApplyExpressionPerRowAlgorithm(ApplyExpressionPerRowAlgorithm other) {
        super(other);
        this.expressionList = new ExpressionTableColumnGeneratorProcessorParameterList(other.expressionList);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        ExpressionVariables variableSet = new ExpressionVariables();
        Map<String, String> annotationsMap = JIPipeTextAnnotation.annotationListToMap(dataBatch.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        variableSet.set("annotations", annotationsMap);
        variableSet.set("num_rows", data.getRowCount());
        for (int col = 0; col < data.getColumnCount(); col++) {
            TableColumn column = data.getColumnReference(col);
            if(column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), Doubles.asList(column.getDataAsDouble(column.getRows())));
            }
            else {
                variableSet.set("all." + column.getLabel(), Arrays.asList(column.getDataAsString(column.getRows())));
            }
        }
        for (ExpressionTableColumnGeneratorProcessor expression : expressionList) {
            List<Object> generatedValues = new ArrayList<>();
            variableSet.set("num_cols", data.getColumnCount());
            variableSet.set("column", data.getColumnIndex(expression.getValue()));
            variableSet.set("column_name", expression.getValue());
            for (int row = 0; row < data.getRowCount(); row++) {
                variableSet.set("row", row);
                for (int col = 0; col < data.getColumnCount(); col++) {
                    variableSet.set(data.getColumnName(col), data.getValueAt(row, col));
                }
                generatedValues.add(expression.getKey().evaluate(variableSet));
            }
            boolean numeric = generatedValues.stream().allMatch(o -> o instanceof Number);
            int targetColumn = data.getOrCreateColumnIndex(expression.getValue(), !numeric);
            for (int row = 0; row < data.getRowCount(); row++) {
                data.setValueAt(generatedValues.get(row), row, targetColumn);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Expressions", description = "Each expression is applied for each row, with variables named according to the column values. " +
            "New columns are created if needed. Existing values are overwritten. The operations are applied in order, meaning that you have access to the results of all previous operations.")
    @JIPipeParameter("expression-list")
    @PairParameterSettings(singleRow = false, keyLabel = "Expression", valueLabel = "Column name")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public ExpressionTableColumnGeneratorProcessorParameterList getExpressionList() {
        return expressionList;
    }

    @JIPipeParameter("expression-list")
    public void setExpressionList(ExpressionTableColumnGeneratorProcessorParameterList expressionList) {
        this.expressionList = expressionList;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {
        private final static Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("<Column>", "Value of the column in the current row", ""));
            VARIABLES.add(new ExpressionParameterVariable("all.<Column>", "Values of all columns", ""));
            VARIABLES.add(new ExpressionParameterVariable("Table column index", "The column index", "column"));
            VARIABLES.add(new ExpressionParameterVariable("Table column name", "The column name", "column_name"));
            VARIABLES.add(new ExpressionParameterVariable("Number of rows", "The number of rows within the table", "num_rows"));
            VARIABLES.add(new ExpressionParameterVariable("Number of columns", "The number of columns within the table", "num_cols"));
            VARIABLES.add(new ExpressionParameterVariable("Annotations", "Map of annotations of the current data batch", "annotations"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
