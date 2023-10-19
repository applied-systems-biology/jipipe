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

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndSortOrderPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.util.SortOrder;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Add missing rows (series)", description = "Adds missing rows in a table that contains a numeric series of rows (e.g., a time series)")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Append")
public class AddMissingRowsInSeriesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpression countingColumn = new StringQueryExpression();
    private ExpressionTableColumnGeneratorProcessorParameterList defaultValues = new ExpressionTableColumnGeneratorProcessorParameterList();

    private OptionalDefaultExpressionParameter minCounter = new OptionalDefaultExpressionParameter();

    private OptionalDefaultExpressionParameter maxCounter = new OptionalDefaultExpressionParameter();

    private double expectedStep = 1.0;

    private boolean ignoreEmptyTables = true;

    public AddMissingRowsInSeriesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AddMissingRowsInSeriesAlgorithm(AddMissingRowsInSeriesAlgorithm other) {
        super(other);
        this.countingColumn = new StringQueryExpression(other.countingColumn);
        this.defaultValues = new ExpressionTableColumnGeneratorProcessorParameterList(other.defaultValues);
        this.minCounter = new OptionalDefaultExpressionParameter(other.minCounter);
        this.maxCounter = new OptionalDefaultExpressionParameter(other.maxCounter);
        this.expectedStep = other.expectedStep;
        this.ignoreEmptyTables = other.ignoreEmptyTables;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        ResultsTableData outputTable;
        if (!ignoreEmptyTables || inputTable.getRowCount() > 0) {
            ExpressionVariables variables = new ExpressionVariables();
            variables.putAnnotations(iterationStep.getMergedTextAnnotations());
            variables.set("num_rows", inputTable.getRowCount());
            variables.set("num_cols", inputTable.getColumnCount());
            variables.set("column_names", inputTable.getColumnNames());
            variables.set("annotations", JIPipeTextAnnotation.annotationListToMap(iterationStep.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting));

            // Calculate counters
            double counterMin = Double.POSITIVE_INFINITY;
            double counterMax = Double.NEGATIVE_INFINITY;
            final int counterColumn = inputTable.getColumnIndex(countingColumn.queryFirst(inputTable.getColumnNames(), variables));

            if (minCounter.isEnabled()) {
                counterMin = minCounter.getContent().evaluateToNumber(variables);
            } else {
                for (int i = 0; i < inputTable.getRowCount(); i++) {
                    counterMin = Math.min(counterMin, inputTable.getValueAsDouble(i, counterColumn));
                }
            }
            if (maxCounter.isEnabled()) {
                counterMax = maxCounter.getContent().evaluateToNumber(variables);
            } else {
                for (int i = 0; i < inputTable.getRowCount(); i++) {
                    counterMax = Math.max(counterMax, inputTable.getValueAsDouble(i, counterColumn));
                }
            }

            // Find missing values
            outputTable = new ResultsTableData(inputTable);
            TDoubleList currentCounters = new TDoubleArrayList();
            for (int i = 0; i < inputTable.getRowCount(); i++) {
                currentCounters.add(inputTable.getValueAsDouble(i, counterColumn));
            }
            currentCounters.sort();

            {
                double min = currentCounters.get(0);
                double max = currentCounters.get(currentCounters.size() - 1);
                if (min > counterMin) {
                    currentCounters.insert(0, counterMin);
                }
                if (max < counterMax) {
                    currentCounters.add(counterMax);
                }
            }

            for (int i = 0; i < currentCounters.size() - 1; i++) {
                double current = currentCounters.get(i);
                double next = currentCounters.get(i + 1);
                if (Math.abs(current - next) > expectedStep) {
                    double newCounter = current + expectedStep;

                    // Generate new row
                    addNewRowForCounter(newCounter, counterColumn, variables, outputTable);
                    currentCounters.insert(i + 1, newCounter);
                }
            }

            // Ensure that the max exists
            if (maxCounter.isEnabled()) {
                currentCounters.clear();
                for (int i = 0; i < inputTable.getRowCount(); i++) {
                    currentCounters.add(inputTable.getValueAsDouble(i, counterColumn));
                }
                if (currentCounters.max() != counterMax) {
                    // Generate new row
                    addNewRowForCounter(counterMax, counterColumn, variables, outputTable);
                }
            }

            // Sort again
            {
                SortTableRowsAlgorithm sortTableRowsAlgorithm = JIPipe.createNode(SortTableRowsAlgorithm.class);
                sortTableRowsAlgorithm.getFirstInputSlot().addData(outputTable, progressInfo);
                sortTableRowsAlgorithm.getSortOrderList().clear();
                StringQueryExpressionAndSortOrderPairParameter pairParameter = new StringQueryExpressionAndSortOrderPairParameter();
                pairParameter.setKey(new StringQueryExpression("\"" + inputTable.getColumnName(counterColumn) + "\""));
                pairParameter.setValue(SortOrder.Ascending);
                sortTableRowsAlgorithm.getSortOrderList().add(pairParameter);
                sortTableRowsAlgorithm.run(progressInfo.resolve("Sort table"));
                outputTable = sortTableRowsAlgorithm.getFirstOutputSlot().getData(0, ResultsTableData.class, progressInfo);
            }
        } else {
            outputTable = inputTable;
        }
        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    private void addNewRowForCounter(double newCounter, int counterColumn, ExpressionVariables variables, ResultsTableData outputTable) {
        int row = outputTable.addRow();
        outputTable.setValueAt(newCounter, row, counterColumn);
        variables.set("counter", newCounter);
        for (ExpressionTableColumnGeneratorProcessor processor : defaultValues) {
            String targetColumn = processor.getValue();
            Object value = processor.getKey().evaluate(variables);
            outputTable.setValueAt(value, row, targetColumn);
        }
    }

    @JIPipeDocumentation(name = "Expected step", description = "The step between two consecutive series points. For example, the step between 5 and 6 is 1. If the calculated step is larger than the provided value, a new row is generated. Must be a positive number.")
    @JIPipeParameter("expected-step")
    public double getExpectedStep() {
        return expectedStep;
    }

    @JIPipeParameter("expected-step")
    public boolean setExpectedStep(double expectedStep) {
        if (expectedStep <= 0)
            return false;
        this.expectedStep = expectedStep;
        return true;
    }

    @JIPipeDocumentation(name = "Ignore empty tables", description = "If enabled, ignore empty tables")
    @JIPipeParameter("ignore-empty-tables")
    public boolean isIgnoreEmptyTables() {
        return ignoreEmptyTables;
    }

    @JIPipeParameter("ignore-empty-tables")
    public void setIgnoreEmptyTables(boolean ignoreEmptyTables) {
        this.ignoreEmptyTables = ignoreEmptyTables;
    }

    @JIPipeDocumentation(name = "Counting column", description = "The column that contains the counter (e.g., the time frame)")
    @JIPipeParameter(value = "counting-column", important = true)
    @ExpressionParameterSettingsVariable(fromClass = CounterVariableSource.class)
    public StringQueryExpression getCountingColumn() {
        return countingColumn;
    }

    @JIPipeParameter("counting-column")
    public void setCountingColumn(StringQueryExpression countingColumn) {
        this.countingColumn = countingColumn;
    }

    @JIPipeDocumentation(name = "Default values", description = "Determines the default values if a row is missing")
    @JIPipeParameter("default-values")
    @ExpressionParameterSettingsVariable(fromClass = DefaultValuesVariableSource.class)
    public ExpressionTableColumnGeneratorProcessorParameterList getDefaultValues() {
        return defaultValues;
    }

    @JIPipeParameter("default-values")
    public void setDefaultValues(ExpressionTableColumnGeneratorProcessorParameterList defaultValues) {
        this.defaultValues = defaultValues;
    }

    @JIPipeDocumentation(name = "Custom minimum counter", description = "If enabled, override the automatically determined minimum counter. Otherwise, the minimum is the minimum in the whole table")
    @JIPipeParameter("min-counter")
    @ExpressionParameterSettingsVariable(fromClass = CounterVariableSource.class)
    public OptionalDefaultExpressionParameter getMinCounter() {
        return minCounter;
    }

    @JIPipeParameter("min-counter")
    public void setMinCounter(OptionalDefaultExpressionParameter minCounter) {
        this.minCounter = minCounter;
    }

    @JIPipeDocumentation(name = "Custom maximum counter", description = "If enabled, override the automatically determined maximum counter. Otherwise, the maximum is the maximum in the whole table")
    @JIPipeParameter("max-counter")
    @ExpressionParameterSettingsVariable(fromClass = CounterVariableSource.class)
    public OptionalDefaultExpressionParameter getMaxCounter() {
        return maxCounter;
    }

    @JIPipeParameter("max-counter")
    public void setMaxCounter(OptionalDefaultExpressionParameter maxCounter) {
        this.maxCounter = maxCounter;
    }

    public static class CounterVariableSource implements ExpressionParameterVariableSource {
        private final static Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Number of rows", "The number of rows within the table", "num_rows"));
            VARIABLES.add(new ExpressionParameterVariable("Number of columns", "The number of columns within the table", "num_cols"));
            VARIABLES.add(new ExpressionParameterVariable("List of column names", "An array of the column names", "column_names"));
            VARIABLES.add(new ExpressionParameterVariable("Annotations", "Map of annotations of the current data batch", "annotations"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }

    public static class DefaultValuesVariableSource implements ExpressionParameterVariableSource {
        private final static Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Counter", "The step counter that will be assigned to the new row", "counter"));
            VARIABLES.add(new ExpressionParameterVariable("List of column names", "An array of the column names", "column_names"));
            VARIABLES.add(new ExpressionParameterVariable("Number of rows", "The number of rows within the table", "num_rows"));
            VARIABLES.add(new ExpressionParameterVariable("Number of columns", "The number of columns within the table", "num_cols"));
            VARIABLES.add(new ExpressionParameterVariable("Annotations", "Map of annotations of the current data batch", "annotations"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
