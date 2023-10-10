package org.hkijena.jipipe.extensions.tables.nodes.transform;

import com.google.common.primitives.Doubles;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TDoubleIntMap;
import gnu.trove.map.hash.TDoubleIntHashMap;
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Table column to histogram", description = "Generates a histogram of table column values")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class TableToHistogramAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private TableColumnSourceExpressionParameter inputColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Column name\"");
    private DefaultExpressionParameter valueFilter = new DefaultExpressionParameter("NOT IS_NAN(value)");
    private DefaultExpressionParameter numBins = new DefaultExpressionParameter("inf");
    private DefaultExpressionParameter outputColumnBinMin = new DefaultExpressionParameter("\"Bin min\"");
    private DefaultExpressionParameter outputColumnBinMax = new DefaultExpressionParameter("\"Bin max\"");
    private DefaultExpressionParameter outputColumnBinCount = new DefaultExpressionParameter("\"Count\"");
    private DefaultExpressionParameter accumulationFunction = new DefaultExpressionParameter("COUNT(values)");
    private boolean cumulative = false;
    private boolean normalize = false;

    public TableToHistogramAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public TableToHistogramAlgorithm(TableToHistogramAlgorithm other) {
        super(other);
        this.inputColumn = new TableColumnSourceExpressionParameter(other.inputColumn);
        this.valueFilter = new DefaultExpressionParameter(other.valueFilter);
        this.numBins = new DefaultExpressionParameter(other.numBins);
        this.outputColumnBinMin = new DefaultExpressionParameter(other.outputColumnBinMin);
        this.outputColumnBinMax = new DefaultExpressionParameter(other.outputColumnBinMax);
        this.outputColumnBinCount = new DefaultExpressionParameter(other.outputColumnBinCount);
        this.accumulationFunction = new DefaultExpressionParameter(other.accumulationFunction);
        this.cumulative = other.cumulative;
        this.normalize = other.normalize;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        TableColumn inputColumn = this.inputColumn.pickOrGenerateColumn(inputTable);
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

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
            dataBatch.addOutputData(getFirstOutputSlot(), dummyTable, progressInfo);
            return;
        }

        double[] inputColumnValues = inputColumn.getDataAsDouble(inputColumn.getRows());
        TDoubleHashSet uniqueFilteredInputColumnValues = new TDoubleHashSet(inputColumnValues.length);
        TDoubleIntMap uniqueFilteredInputColumnValuesCounts = new TDoubleIntHashMap();

        // Filter values
        for (double value : inputColumnValues) {
            variables.set("value", value);
            if (valueFilter.evaluateToBoolean(variables)) {
                uniqueFilteredInputColumnValues.add(value);
                uniqueFilteredInputColumnValuesCounts.adjustOrPutValue(value, 1, 1);
            }
        }

        if (uniqueFilteredInputColumnValues.isEmpty()) {
            progressInfo.log("All values were filtered! Skipping.");
            dataBatch.addOutputData(getFirstOutputSlot(), dummyTable, progressInfo);
            return;
        }

        // Get number of bins
        double nBins_ = numBins.evaluateToDouble(variables);
        int nBins;
        if (Double.isInfinite(nBins_)) {
            // Same as unique values
            nBins = uniqueFilteredInputColumnValues.size();
        } else {
            nBins = (int) nBins_;
        }

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
                    if(value >= currentStart && value <= currentEnd && !usedSortedUniqueFilteredInputColumnValues.contains(value)) {
                        values.add(value);
                        usedSortedUniqueFilteredInputColumnValues.add(value);
                    }
                }
                if(!values.isEmpty()) {
                    generatedBins.add(values);
                }
                currentStart += binWidth;
                currentEnd += binWidth;
            }
            while (currentEnd <= maxValue);
        }

        // Generate intermediate arrays
        double[] binMinValues = new double[generatedBins.size()];
        double[] binMaxValues = new double[generatedBins.size()];
        double[] binCounts = new double[generatedBins.size()];

        for (int i = 0; i < generatedBins.size(); i++) {
            TDoubleList bin = generatedBins.get(i);
            binMinValues[i] = bin.min();
            binMaxValues[i] = bin.max();
            List<Double> allValues = new ArrayList<>();
            for (int j = 0; j < bin.size(); j++) {
                double value = bin.get(j);
                int count = uniqueFilteredInputColumnValuesCounts.get(value);
                for (int k = 0; k < count; k++) {
                    allValues.add(value);
                }
            }
            variables.set("values", allValues);
            binCounts[i] = accumulationFunction.evaluateToDouble(variables);
        }

        // Cumulative
        if(cumulative) {
            for (int i = 1; i < binCounts.length; i++) {
                binCounts[i] += binCounts[i - 1];
            }
        }

        // Normalize
        if(normalize) {
            double max = Doubles.max(binCounts);
            for (int i = 0; i < binCounts.length; i++) {
                binCounts[i] = binCounts[i] / max;
            }
        }

        ResultsTableData outputTable = new ResultsTableData();
        outputTable.addNumericColumn(outputBinMinColumName);
        outputTable.addNumericColumn(outputBinMaxColumName);
        outputTable.addNumericColumn(outputBinCountColumName);

        for (int i = 0; i < binCounts.length; i++) {
            outputTable.addRow();
            outputTable.setValueAt(binMinValues[i], i, outputBinMinColumName);
            outputTable.setValueAt(binMaxValues[i], i, outputBinMaxColumName);
            outputTable.setValueAt(binCounts[i], i, outputBinCountColumName);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @JIPipeDocumentation(name = "Input column", description = "The column to be used for creating a histogram")
    @JIPipeParameter(value = "input-column", uiOrder = -100, important = true)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public TableColumnSourceExpressionParameter getInputColumn() {
        return inputColumn;
    }

    @JIPipeParameter("input-column")
    public void setInputColumn(TableColumnSourceExpressionParameter inputColumn) {
        this.inputColumn = inputColumn;
    }

    @JIPipeDocumentation(name = "Value filter", description = "Allows to remove values before they are included into the histogram calculations")
    @JIPipeParameter("value-filter")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "value", description = "The value to be tested", name = "Value")
    public DefaultExpressionParameter getValueFilter() {
        return valueFilter;
    }

    @JIPipeParameter("value-filter")
    public void setValueFilter(DefaultExpressionParameter valueFilter) {
        this.valueFilter = valueFilter;
    }

    @JIPipeDocumentation(name = "Number of bins", description = "The number of bins. If set to infinite (<code>inf</code>), there will be as many bins as unique values")
    @JIPipeParameter("num-bins")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getNumBins() {
        return numBins;
    }

    @JIPipeParameter("num-bins")
    public void setNumBins(DefaultExpressionParameter numBins) {
        this.numBins = numBins;
    }

    @JIPipeDocumentation(name = "Accumulation function", description = "Function that accumulates the values inside a bin")
    @ExpressionParameterSettingsVariable(key = "values", description = "The values to be accumulated", name = "Values")
    @JIPipeParameter("accumulation-function")
    public DefaultExpressionParameter getAccumulationFunction() {
        return accumulationFunction;
    }

    @JIPipeParameter("accumulation-function")
    public void setAccumulationFunction(DefaultExpressionParameter accumulationFunction) {
        this.accumulationFunction = accumulationFunction;
    }

    @JIPipeDocumentation(name = "Output column (bin min)", description = "The column where the minimum value of a bin will be written")
    @JIPipeParameter(value = "output-column-bin-min", uiOrder = -90)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getOutputColumnBinMin() {
        return outputColumnBinMin;
    }

    @JIPipeParameter("output-column-bin-min")
    public void setOutputColumnBinMin(DefaultExpressionParameter outputColumnBinMin) {
        this.outputColumnBinMin = outputColumnBinMin;
    }

    @JIPipeDocumentation(name = "Output column (bin max)", description = "The column where the maximum value of a bin will be written")
    @JIPipeParameter(value = "output-column-bin-max", uiOrder = -80)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getOutputColumnBinMax() {
        return outputColumnBinMax;
    }

    @JIPipeParameter("output-column-bin-max")
    public void setOutputColumnBinMax(DefaultExpressionParameter outputColumnBinMax) {
        this.outputColumnBinMax = outputColumnBinMax;
    }

    @JIPipeDocumentation(name = "Output column (accumulated)", description = "The [normalized/cumulative] accumulated values in the bin")
    @JIPipeParameter(value = "output-column-bin-count", uiOrder = -70)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getOutputColumnBinCount() {
        return outputColumnBinCount;
    }

    @JIPipeParameter("output-column-bin-count")
    public void setOutputColumnBinCount(DefaultExpressionParameter outputColumnBinCount) {
        this.outputColumnBinCount = outputColumnBinCount;
    }

    @JIPipeDocumentation(name = "Cumulative", description = "Generate a cumulative histogram. Applied after accumulation.")
    @JIPipeParameter("cumulative")
    public boolean isCumulative() {
        return cumulative;
    }

    @JIPipeParameter("cumulative")
    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
    }

    @JIPipeDocumentation(name = "Normalize", description = "Normalize the histogram by dividing the accumulated values by the maximum accumulated value. Applied after accumulation and calculating cumulative values (if enabled).")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }
}
