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

package org.hkijena.jipipe.plugins.tables.nodes.columns;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.tables.datatypes.*;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Apply expression to table (columns)", description = "Applies an expression to the whole table. Each entry in the list of processors generates a column.")
@AddJIPipeNodeAlias(aliasName = "Generate table columns from expression", nodeTypeCategory = DataSourceNodeTypeCategory.class)
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true, optional = true, description = "The optional input table. If none is provided, the number of rows and columns are set to zero.")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ApplyExpressionToTableByColumnAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList processorParameters = ParameterCollectionList.containingCollection(ProcessingItem.class);

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ApplyExpressionToTableByColumnAlgorithm(JIPipeNodeInfo info) {
        super(info);
        processorParameters.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ApplyExpressionToTableByColumnAlgorithm(ApplyExpressionToTableByColumnAlgorithm other) {
        super(other);
        this.processorParameters = new ParameterCollectionList(other.processorParameters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<TableColumn> resultColumns = new ArrayList<>();

        // Copy annotations
        JIPipeExpressionVariablesMap expressionVariables = new JIPipeExpressionVariablesMap();
        Map<String, String> annotationsMap = JIPipeTextAnnotation.annotationListToMap(iterationStep.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        expressionVariables.set("annotations", annotationsMap);

        getDefaultCustomExpressionVariables().writeToVariables(expressionVariables);

        // Copy columns
        if (input != null) {
            for (int col = 0; col < input.getColumnCount(); col++) {
                TableColumn column = input.getColumnReference(col);
                if (column.isNumeric()) {
                    expressionVariables.set(column.getLabel(), Doubles.asList(column.getDataAsDouble(column.getRows())));
                } else {
                    expressionVariables.set(column.getLabel(), Arrays.asList(column.getDataAsString(column.getRows())));
                }
            }
        }

        for (ProcessingItem item : processorParameters.mapToCollection(ProcessingItem.class)) {
            if (input != null) {
                expressionVariables.set("num_rows", input.getRowCount());
                expressionVariables.set("num_cols", input.getColumnCount());
            } else {
                expressionVariables.set("num_rows", 0);
                expressionVariables.set("num_cols", 0);
            }
            String outputColumnName = item.outputColumnName.evaluateToString(expressionVariables);
            Object result = item.outputColumnValues.evaluate(expressionVariables);
            TableColumn resultColumn;
            if (result instanceof Number) {
                resultColumn = new DoubleArrayTableColumn(new double[]{((Number) result).doubleValue()}, outputColumnName);
            } else if (result instanceof Collection) {
                if (((Collection<?>) result).stream().allMatch(v -> v instanceof Number)) {
                    double[] data = new double[((Collection<?>) result).size()];
                    int i = 0;
                    for (Object o : (Collection<?>) result) {
                        data[i] = ((Number) o).doubleValue();
                        ++i;
                    }
                    resultColumn = new DoubleArrayTableColumn(data, outputColumnName);
                } else {
                    String[] data = new String[((Collection<?>) result).size()];
                    int i = 0;
                    for (Object o : (Collection<?>) result) {
                        data[i] = StringUtils.nullToEmpty(o);
                        ++i;
                    }
                    resultColumn = new StringArrayTableColumn(data, outputColumnName);
                }
            } else {
                resultColumn = new StringArrayTableColumn(new String[]{StringUtils.nullToEmpty(result)}, outputColumnName);
            }
            resultColumns.add(new RelabeledTableColumn(resultColumn, outputColumnName));
        }

        // Combine into one table
        ResultsTableData output = new ResultsTableData(resultColumns);
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Column generators", description = "Each item in the list of generators should output a single value or a list of values, which will be the items in the generated column")
    @JIPipeParameter("processor-parameters")
    public ParameterCollectionList getProcessorParameters() {
        return processorParameters;
    }

    @JIPipeParameter("processor-parameters")
    public void setProcessorParameters(ParameterCollectionList processorParameters) {
        this.processorParameters = processorParameters;
    }

    public static class ProcessingItem extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter outputColumnName = new JIPipeExpressionParameter("\"Output\"");
        private JIPipeExpressionParameter outputColumnValues = new JIPipeExpressionParameter("");

        @SetJIPipeDocumentation(name = "Output column name")
        @JIPipeParameter("name")
        @AddJIPipeExpressionParameterVariable(name = "<Column>", description = "All values of the column in the input table as list", key = "")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Annotations", description = "Map of text annotations", key = "annotations")
        @AddJIPipeExpressionParameterVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @AddJIPipeExpressionParameterVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        public JIPipeExpressionParameter getOutputColumnName() {
            return outputColumnName;
        }

        @JIPipeParameter("name")
        public void setOutputColumnName(JIPipeExpressionParameter outputColumnName) {
            this.outputColumnName = outputColumnName;
        }

        @SetJIPipeDocumentation(name = "Output column values")
        @JIPipeParameter("value")
        @AddJIPipeExpressionParameterVariable(name = "<Column>", description = "All values of the column in the input table as list", key = "")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Number of rows", description = "The number of rows within the table", key = "num_rows")
        @AddJIPipeExpressionParameterVariable(name = "Number of columns", description = "The number of columns within the table", key = "num_cols")
        @AddJIPipeExpressionParameterVariable(name = "Annotations", description = "Map of text annotations", key = "annotations")
        public JIPipeExpressionParameter getOutputColumnValues() {
            return outputColumnValues;
        }

        @JIPipeParameter("value")
        public void setOutputColumnValues(JIPipeExpressionParameter outputColumnValues) {
            this.outputColumnValues = outputColumnValues;
        }
    }
}
