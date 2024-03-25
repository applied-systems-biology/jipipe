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

package org.hkijena.jipipe.plugins.tables.nodes.rows;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.plugins.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.plugins.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

import java.util.*;

@SetJIPipeDocumentation(name = "Apply expression per row", description = "Deprecated. Use the node with the same name. Applies an expression for each row. The column values are available as variables.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
@Deprecated
@LabelAsJIPipeHidden
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        Map<String, String> annotationsMap = JIPipeTextAnnotation.annotationListToMap(iterationStep.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        variableSet.set("annotations", annotationsMap);
        getDefaultCustomExpressionVariables().writeToVariables(variableSet);
        variableSet.set("num_rows", data.getRowCount());
        for (int col = 0; col < data.getColumnCount(); col++) {
            TableColumn column = data.getColumnReference(col);
            if (column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), Doubles.asList(column.getDataAsDouble(column.getRows())));
            } else {
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
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Expressions", description = "Each expression is applied for each row, with variables named according to the column values. " +
            "New columns are created if needed. Existing values are overwritten. The operations are applied in order, meaning that you have access to the results of all previous operations.")
    @JIPipeParameter("expression-list")
    @PairParameterSettings(singleRow = false, keyLabel = "Expression", valueLabel = "Column name")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public ExpressionTableColumnGeneratorProcessorParameterList getExpressionList() {
        return expressionList;
    }

    @JIPipeParameter("expression-list")
    public void setExpressionList(ExpressionTableColumnGeneratorProcessorParameterList expressionList) {
        this.expressionList = expressionList;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {
        private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("", "<Column>", "Value of the column in the current row"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("", "all.<Column>", "Values of all columns"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("column", "Table column index", "The column index"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("column_name", "Table column name", "The column name"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_rows", "Number of rows", "The number of rows within the table"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_cols", "Number of columns", "The number of columns within the table"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Annotations", "Map of annotations of the current data batch"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
