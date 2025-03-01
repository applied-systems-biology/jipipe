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

package org.hkijena.jipipe.plugins.tables.nodes.transform;

import com.google.common.primitives.Doubles;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Table column to histogram", description = "Deprecated. Replace with a new node with the same name. " +
        "Generates a histogram of table column values. Also supports weighting the values via an additional column.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
@LabelAsJIPipeHidden
@Deprecated
public class TableToHistogramAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private TableColumnSourceExpressionParameter inputColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Column name\"");
    private TableColumnSourceExpressionParameter weightColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "1");
    private JIPipeExpressionParameter valueFilter = new JIPipeExpressionParameter("NOT IS_NAN(value)");
    private JIPipeExpressionParameter numBins = new JIPipeExpressionParameter("inf");
    private JIPipeExpressionParameter outputColumnBinMin = new JIPipeExpressionParameter("\"Bin min\"");
    private JIPipeExpressionParameter outputColumnBinMax = new JIPipeExpressionParameter("\"Bin max\"");
    private JIPipeExpressionParameter outputColumnBinCount = new JIPipeExpressionParameter("\"Count\"");
    private JIPipeExpressionParameter accumulationFunction = new JIPipeExpressionParameter("COUNT(values)");
    private ParameterCollectionList additionalColumns = ParameterCollectionList.containingCollection(AdditionalColumn.class);
    private boolean cumulative = false;
    private boolean normalize = false;

    public TableToHistogramAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TableToHistogramAlgorithm(TableToHistogramAlgorithm other) {
        super(other);
        this.inputColumn = new TableColumnSourceExpressionParameter(other.inputColumn);
        this.weightColumn = new TableColumnSourceExpressionParameter(other.weightColumn);
        this.valueFilter = new JIPipeExpressionParameter(other.valueFilter);
        this.numBins = new JIPipeExpressionParameter(other.numBins);
        this.outputColumnBinMin = new JIPipeExpressionParameter(other.outputColumnBinMin);
        this.outputColumnBinMax = new JIPipeExpressionParameter(other.outputColumnBinMax);
        this.outputColumnBinCount = new JIPipeExpressionParameter(other.outputColumnBinCount);
        this.accumulationFunction = new JIPipeExpressionParameter(other.accumulationFunction);
        this.additionalColumns = new ParameterCollectionList(other.additionalColumns);
        this.cumulative = other.cumulative;
        this.normalize = other.normalize;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

        TableColumnData inputColumn = this.inputColumn.pickOrGenerateColumn(inputTable, variables);
        TableColumnData weightColumn = this.weightColumn.pickOrGenerateColumn(inputTable, variables);

        // Calculate output column names
        String outputBinMinColumName = outputColumnBinMin.evaluateToString(variables);
        String outputBinMaxColumName = outputColumnBinMax.evaluateToString(variables);
        String outputBinCountColumName = outputColumnBinCount.evaluateToString(variables);

        // Create dummy table
        ResultsTableData dummyTable = new ResultsTableData();
        dummyTable.addNumericColumn(outputBinMinColumName);
        dummyTable.addNumericColumn(outputBinMaxColumName);
        dummyTable.addNumericColumn(outputBinCountColumName);

        if (inputColumn.getRows() == 0) {
            progressInfo.log("Input table is empty! Skipping.");
            iterationStep.addOutputData(getFirstOutputSlot(), dummyTable, progressInfo);
            return;
        }

        double[] inputColumnValues = inputColumn.getDataAsDouble(inputColumn.getRows());
        double[] weightColumnValues = weightColumn.getDataAsDouble(inputColumn.getRows());
        TDoubleHashSet uniqueFilteredInputColumnValues = new TDoubleHashSet(inputColumnValues.length);
        TDoubleDoubleMap uniqueFilteredInputColumnValuesCounts = new TDoubleDoubleHashMap();

        // Filter values
        for (int i = 0; i < inputColumnValues.length; i++) {
            double value = inputColumnValues[i];
            double weight = weightColumnValues[i];
            variables.set("value", value);
            variables.set("weight", weight);
            if (valueFilter.evaluateToBoolean(variables)) {
                uniqueFilteredInputColumnValues.add(value);
                uniqueFilteredInputColumnValuesCounts.adjustOrPutValue(value, weight, weight);
            }
        }

        if (uniqueFilteredInputColumnValues.isEmpty()) {
            progressInfo.log("All values were filtered! Skipping.");
            iterationStep.addOutputData(getFirstOutputSlot(), dummyTable, progressInfo);
            return;
        }

        variables.put("filtered_unique_values", Doubles.asList(uniqueFilteredInputColumnValues.toArray()));

        // Get number of bins
        double nBins_ = numBins.evaluateToDouble(variables);
        int nBins;
        if (Double.isInfinite(nBins_)) {
            // Same as unique values
            nBins = uniqueFilteredInputColumnValues.size();
        } else {
            nBins = (int) nBins_;
        }

        progressInfo.log("Number of bins = " + nBins);

        // Determine the bins
        TDoubleList sortedUniqueFilteredInputColumnValues = new TDoubleArrayList(uniqueFilteredInputColumnValues);
        sortedUniqueFilteredInputColumnValues.sort();
        TDoubleSet usedSortedUniqueFilteredInputColumnValues = new TDoubleHashSet();

        List<TDoubleList> generatedBins = new ArrayList<>();

        {
            double minValue = sortedUniqueFilteredInputColumnValues.min();
            double maxValue = sortedUniqueFilteredInputColumnValues.max();
            double binWidth = (maxValue - minValue) / nBins;

            double currentStart = minValue;
            double currentEnd = currentStart + binWidth;
            do {

                TDoubleList values = new TDoubleArrayList();
                for (int i = 0; i < sortedUniqueFilteredInputColumnValues.size(); i++) {
                    double value = sortedUniqueFilteredInputColumnValues.get(i);
                    if (value >= currentStart && value <= currentEnd && !usedSortedUniqueFilteredInputColumnValues.contains(value)) {
                        values.add(value);
                        usedSortedUniqueFilteredInputColumnValues.add(value);
                    }
                }
                if (!values.isEmpty()) {
                    generatedBins.add(values);
                }
                currentStart += binWidth;
                currentEnd += binWidth;
                System.out.println(currentStart + ",  " + currentEnd + " , " + maxValue);
            }
            while (currentEnd <= maxValue && binWidth > 0);
        }

        // Generate intermediate arrays
        double[] binMinValues = new double[generatedBins.size()];
        double[] binMaxValues = new double[generatedBins.size()];
        Map<String, double[]> additionalColumns = new HashMap<>();
        List<String> additionalColumnNames = new ArrayList<>();
        double[] binCounts = new double[generatedBins.size()];

        for (AdditionalColumn column : this.additionalColumns.mapToCollection(AdditionalColumn.class)) {
            String name = column.outputColumnName.evaluateToString(variables);
            additionalColumnNames.add(name);
        }

        for (int i = 0; i < generatedBins.size(); i++) {
            TDoubleList bin = generatedBins.get(i);
            binMinValues[i] = bin.min();
            binMaxValues[i] = bin.max();

            // Calculate bin counts (all values)
            List<Double> allValues = new ArrayList<>();
            for (int j = 0; j < bin.size(); j++) {
                double value = bin.get(j);
                double count = uniqueFilteredInputColumnValuesCounts.get(value);
                for (int k = 0; k < count; k++) {
                    allValues.add(value);
                }
            }

            variables.set("values", allValues);
            binCounts[i] = accumulationFunction.evaluateToDouble(variables);
            // Calculate additional columns

            for (AdditionalColumn column : this.additionalColumns.mapToCollection(AdditionalColumn.class)) {
                String name = column.outputColumnName.evaluateToString(variables);
                double value = column.outputColumnValues.evaluateToDouble(variables);

                double[] values = additionalColumns.computeIfAbsent(name, k -> new double[generatedBins.size()]);
                values[i] = value;
            }
        }

        // Cumulative
        if (cumulative) {
            for (int i = 1; i < binCounts.length; i++) {
                binCounts[i] += binCounts[i - 1];
            }
        }

        // Normalize
        if (normalize) {
            double max = Doubles.max(binCounts);
            for (int i = 0; i < binCounts.length; i++) {
                binCounts[i] = binCounts[i] / max;
            }
        }

        ResultsTableData outputTable = new ResultsTableData();
        outputTable.addNumericColumn(outputBinMinColumName);
        outputTable.addNumericColumn(outputBinMaxColumName);
        for (String columnName : additionalColumnNames) {
            outputTable.addNumericColumn(columnName);
        }
        outputTable.addNumericColumn(outputBinCountColumName);


        for (int i = 0; i < binCounts.length; i++) {
            outputTable.addRow();
            outputTable.setValueAt(binMinValues[i], i, outputBinMinColumName);
            outputTable.setValueAt(binMaxValues[i], i, outputBinMaxColumName);
            outputTable.setValueAt(binCounts[i], i, outputBinCountColumName);
            for (Map.Entry<String, double[]> entry : additionalColumns.entrySet()) {
                outputTable.setValueAt(entry.getValue()[i], i, entry.getKey());
            }
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Additional columns", description = "Additional columns to be added into the histogram.")
    @JIPipeParameter("additional-columns")
    public ParameterCollectionList getAdditionalColumns() {
        return additionalColumns;
    }

    @JIPipeParameter("additional-columns")
    public void setAdditionalColumns(ParameterCollectionList additionalColumns) {
        this.additionalColumns = additionalColumns;
    }

    @SetJIPipeDocumentation(name = "Input column", description = "The column to be used for creating a histogram")
    @JIPipeParameter(value = "input-column", uiOrder = -100, important = true)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public TableColumnSourceExpressionParameter getInputColumn() {
        return inputColumn;
    }

    @JIPipeParameter("input-column")
    public void setInputColumn(TableColumnSourceExpressionParameter inputColumn) {
        this.inputColumn = inputColumn;
    }

    @SetJIPipeDocumentation(name = "Weight column", description = "Column containing the weight value for each value")
    @JIPipeParameter(value = "weight-column", important = true, uiOrder = -99)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public TableColumnSourceExpressionParameter getWeightColumn() {
        return weightColumn;
    }

    @JIPipeParameter("weight-column")
    public void setWeightColumn(TableColumnSourceExpressionParameter weightColumn) {
        this.weightColumn = weightColumn;
    }

    @SetJIPipeDocumentation(name = "Value filter", description = "Allows to remove values before they are included into the histogram calculations")
    @JIPipeParameter("value-filter")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "value", description = "The value to be tested", name = "Value")
    @AddJIPipeExpressionParameterVariable(key = "weight", description = "The weight of the value", name = "Weight")
    public JIPipeExpressionParameter getValueFilter() {
        return valueFilter;
    }

    @JIPipeParameter("value-filter")
    public void setValueFilter(JIPipeExpressionParameter valueFilter) {
        this.valueFilter = valueFilter;
    }

    @SetJIPipeDocumentation(name = "Number of bins", description = "The number of bins. If set to infinite (<code>inf</code>), there will be as many bins as unique values")
    @JIPipeParameter("num-bins")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "filtered_unique_values", name = "Filtered unique values", description = "Unique values that passed the filter")
    public JIPipeExpressionParameter getNumBins() {
        return numBins;
    }

    @JIPipeParameter("num-bins")
    public void setNumBins(JIPipeExpressionParameter numBins) {
        this.numBins = numBins;
    }

    @SetJIPipeDocumentation(name = "Accumulation function", description = "Function that accumulates the values inside a bin")
    @AddJIPipeExpressionParameterVariable(key = "values", description = "The values to be accumulated", name = "Values")
    @JIPipeParameter("accumulation-function")
    public JIPipeExpressionParameter getAccumulationFunction() {
        return accumulationFunction;
    }

    @JIPipeParameter("accumulation-function")
    public void setAccumulationFunction(JIPipeExpressionParameter accumulationFunction) {
        this.accumulationFunction = accumulationFunction;
    }

    @SetJIPipeDocumentation(name = "Output column (bin min)", description = "The column where the minimum value of a bin will be written")
    @JIPipeParameter(value = "output-column-bin-min", uiOrder = -90)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getOutputColumnBinMin() {
        return outputColumnBinMin;
    }

    @JIPipeParameter("output-column-bin-min")
    public void setOutputColumnBinMin(JIPipeExpressionParameter outputColumnBinMin) {
        this.outputColumnBinMin = outputColumnBinMin;
    }

    @SetJIPipeDocumentation(name = "Output column (bin max)", description = "The column where the maximum value of a bin will be written")
    @JIPipeParameter(value = "output-column-bin-max", uiOrder = -80)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getOutputColumnBinMax() {
        return outputColumnBinMax;
    }

    @JIPipeParameter("output-column-bin-max")
    public void setOutputColumnBinMax(JIPipeExpressionParameter outputColumnBinMax) {
        this.outputColumnBinMax = outputColumnBinMax;
    }

    @SetJIPipeDocumentation(name = "Output column (accumulated)", description = "The [normalized/cumulative] accumulated values in the bin")
    @JIPipeParameter(value = "output-column-bin-count", uiOrder = -70)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getOutputColumnBinCount() {
        return outputColumnBinCount;
    }

    @JIPipeParameter("output-column-bin-count")
    public void setOutputColumnBinCount(JIPipeExpressionParameter outputColumnBinCount) {
        this.outputColumnBinCount = outputColumnBinCount;
    }

    @SetJIPipeDocumentation(name = "Cumulative", description = "Generate a cumulative histogram. Applied after accumulation.")
    @JIPipeParameter("cumulative")
    public boolean isCumulative() {
        return cumulative;
    }

    @JIPipeParameter("cumulative")
    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
    }

    @SetJIPipeDocumentation(name = "Normalize", description = "Normalize the histogram by dividing the accumulated values by the maximum accumulated value. Applied after accumulation and calculating cumulative values (if enabled).")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    public static class AdditionalColumn extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter outputColumnName = new JIPipeExpressionParameter("\"Output\"");
        private JIPipeExpressionParameter outputColumnValues = new JIPipeExpressionParameter("");

        @SetJIPipeDocumentation(name = "Output column name")
        @JIPipeParameter("name")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getOutputColumnName() {
            return outputColumnName;
        }

        @JIPipeParameter("name")
        public void setOutputColumnName(JIPipeExpressionParameter outputColumnName) {
            this.outputColumnName = outputColumnName;
        }

        @SetJIPipeDocumentation(name = "Output column values")
        @JIPipeParameter("value")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Bin values", key = "values", description = "The list of values inside the bin")
        public JIPipeExpressionParameter getOutputColumnValues() {
            return outputColumnValues;
        }

        @JIPipeParameter("value")
        public void setOutputColumnValues(JIPipeExpressionParameter outputColumnValues) {
            this.outputColumnValues = outputColumnValues;
        }
    }
}
