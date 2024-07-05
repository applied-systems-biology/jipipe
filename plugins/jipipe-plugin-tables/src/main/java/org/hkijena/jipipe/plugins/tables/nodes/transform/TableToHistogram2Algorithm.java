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
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
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
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

import java.util.*;

@SetJIPipeDocumentation(name = "Table column to histogram", description = "Generates a histogram of table column values. Also supports weighting the values via an additional column.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class TableToHistogram2Algorithm extends JIPipeSimpleIteratingAlgorithm {
    private TableColumnSourceExpressionParameter inputColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"Column name\"");
    private TableColumnSourceExpressionParameter weightColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.Generate, "1");
    private JIPipeExpressionParameter valueFilter = new JIPipeExpressionParameter("NOT IS_NAN(value)");
    private JIPipeExpressionParameter numBins = new JIPipeExpressionParameter("inf");
    private ParameterCollectionList outputColumns = ParameterCollectionList.containingCollection(OutputColumn.class);

    public TableToHistogram2Algorithm(JIPipeNodeInfo info) {
        super(info);
        outputColumns.addFromTemplate(new OutputColumn(new JIPipeExpressionParameter("\"Min\""), new JIPipeExpressionParameter("bin_limit_min")));
        outputColumns.addFromTemplate(new OutputColumn(new JIPipeExpressionParameter("\"Max\""), new JIPipeExpressionParameter("bin_limit_max")));
        outputColumns.addFromTemplate(new OutputColumn(new JIPipeExpressionParameter("\"Median\""), new JIPipeExpressionParameter("MEDIAN(values)")));
        outputColumns.addFromTemplate(new OutputColumn(new JIPipeExpressionParameter("\"Count\""), new JIPipeExpressionParameter("count")));
        outputColumns.addFromTemplate(new OutputColumn(new JIPipeExpressionParameter("\"Weighted Count\""), new JIPipeExpressionParameter("weighted_count")));
        outputColumns.addFromTemplate(new OutputColumn(new JIPipeExpressionParameter("\"Cumulative Count\""), new JIPipeExpressionParameter("cumulative_count")));
        outputColumns.addFromTemplate(new OutputColumn(new JIPipeExpressionParameter("\"Cumulative Weighted Count\""), new JIPipeExpressionParameter("cumulative_weighted_count")));
    }

    public TableToHistogram2Algorithm(TableToHistogram2Algorithm other) {
        super(other);
        this.inputColumn = new TableColumnSourceExpressionParameter(other.inputColumn);
        this.weightColumn = new TableColumnSourceExpressionParameter(other.weightColumn);
        this.valueFilter = new JIPipeExpressionParameter(other.valueFilter);
        this.numBins = new JIPipeExpressionParameter(other.numBins);
        this.outputColumns = new ParameterCollectionList(other.outputColumns);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        TableColumn inputColumn = this.inputColumn.pickOrGenerateColumn(inputTable, variables);
        TableColumn weightColumn = this.weightColumn.pickOrGenerateColumn(inputTable, variables);

        double[] inputColumnValues = inputColumn.getDataAsDouble(inputColumn.getRows());
        double[] weightColumnValues = weightColumn.getDataAsDouble(inputColumn.getRows());
        TDoubleHashSet allowedValues = new TDoubleHashSet(inputColumnValues.length);

        // Filter values
        for (double value : inputColumnValues) {
            variables.set("value", value);
            if (valueFilter.evaluateToBoolean(variables)) {
                allowedValues.add(value);
            }
        }

        variables.put("filtered_unique_values", Doubles.asList(allowedValues.toArray()));

        // Get number of bins
        double nBins_ = numBins.evaluateToDouble(variables);
        int bins;
        if (Double.isInfinite(nBins_)) {
            // Same as unique values
            bins = allowedValues.size();
        } else {
            bins = (int) nBins_;
        }
        progressInfo.log("Number of bins = " + bins);

        // Find maximum and minimum value and bin width
        final double maximum = Doubles.max(allowedValues.toArray());
        final double minimum = Doubles.min(allowedValues.toArray());

        double binWidth = (maximum - minimum) / bins;

        // Find histogram bins
        double lower = minimum;
        double upper;
        List<HistogramBin> binList = new ArrayList<>(bins);
        for (int i = 0; i < bins; i++) {
            HistogramBin bin;
            // make sure bins[bins.length]'s upper boundary ends at maximum
            // to avoid the rounding issue. the bins[0] lower boundary is
            // guaranteed start from min
            if (i == bins - 1) {
                bin = new HistogramBin(lower, maximum);
            }
            else {
                upper = minimum + (i + 1) * binWidth;
                bin = new HistogramBin(lower, upper);
                lower = upper;
            }
            binList.add(bin);
        }

        // fill the bins
        for (int i = 0; i < inputColumnValues.length; i++) {
            int binIndex = bins - 1;
            double value = inputColumnValues[i];
            double weight = weightColumnValues[i];
            if(allowedValues.contains(value)) {
                if (value < maximum) {
                    double fraction = (value - minimum) / (maximum - minimum);
                    if (fraction < 0.0) {
                        fraction = 0.0;
                    }
                    binIndex = (int) (fraction * bins);
                    // rounding could result in binIndex being equal to bins
                    // which will cause an IndexOutOfBoundsException - see bug
                    // report 1553088
                    if (binIndex >= bins) {
                        binIndex = bins - 1;
                    }
                }
                HistogramBin bin = binList.get(binIndex);
                bin.increment(value, weight);
            }
        }

        // Collect additional column names
        List<OutputColumn> mappedOutputColumns = outputColumns.mapToCollection(OutputColumn.class);
        List<String> outputColumnNames = new ArrayList<>();
        for (OutputColumn column : mappedOutputColumns) {
            String name = column.outputColumnName.evaluateToString(variables);
            outputColumnNames.add(name);
        }

        // Calculate cumulative counts
        for (int i = 0; i < binList.size(); i++) {
            if(i > 0) {
                HistogramBin previousHistogramBin = binList.get(i - 1);
                HistogramBin currentHistogramBin = binList.get(i);

                currentHistogramBin.setCumulativeCount(previousHistogramBin.getCumulativeCount() + currentHistogramBin.getCount());
                currentHistogramBin.setCumulativeWeightedCount(previousHistogramBin.getCumulativeWeightedCount() + currentHistogramBin.getWeightedCount());
            }
            else {
                HistogramBin firstHistogramBin = binList.get(i);
                firstHistogramBin.setCumulativeCount(firstHistogramBin.getCount());
                firstHistogramBin.setCumulativeWeightedCount(firstHistogramBin.getWeightedCount());
            }
        }

        // Calculate max values
        final double maxCount = binList.stream().map(HistogramBin::getCount).max(Comparator.naturalOrder()).orElse(Double.NEGATIVE_INFINITY);
        final double maxWeightedCount = binList.stream().map(HistogramBin::getWeightedCount).max(Comparator.naturalOrder()).orElse(Double.NEGATIVE_INFINITY);
        final double maxCumulativeCount = binList.stream().map(HistogramBin::getCumulativeCount).max(Comparator.naturalOrder()).orElse(Double.NEGATIVE_INFINITY);
        final double maxCumulativeWeightedCount = binList.stream().map(HistogramBin::getCumulativeWeightedCount).max(Comparator.naturalOrder()).orElse(Double.NEGATIVE_INFINITY);


        // Create output table
        ResultsTableData outputTable = new ResultsTableData();
        for (String columnName : outputColumnNames) {
            outputTable.addNumericColumn(columnName);
        }


        for (HistogramBin histogramBin : binList) {
            variables.set("values", Doubles.asList(histogramBin.getValues().toArray()));
            variables.set("unique_values", Doubles.asList(histogramBin.getUniqueValues().toArray()));
            variables.set("count", histogramBin.getCount());
            variables.set("weighted_count", histogramBin.getWeightedCount());
            variables.set("cumulative_count", histogramBin.getCumulativeCount());
            variables.set("cumulative_weighted_count", histogramBin.getCumulativeWeightedCount());
            variables.set("max_count", maxCount);
            variables.set("max_weighted_count", maxWeightedCount);
            variables.set("max_cumulative_count", maxCumulativeCount);
            variables.set("max_cumulative_weighted_count", maxCumulativeWeightedCount);
            variables.set("bin_limit_min", histogramBin.getStartBoundary());
            variables.set("bin_limit_max", histogramBin.getEndBoundary());
            Map<String, Object> rowValues = new HashMap<>();
            for (int i = 0; i < mappedOutputColumns.size(); i++) {
                String name = outputColumnNames.get(i);
                OutputColumn outputColumn = mappedOutputColumns.get(i);
                double value = outputColumn.outputColumnValues.evaluateToDouble(variables);
                rowValues.put(name, value);
            }
            outputTable.addRow(rowValues);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Output columns", description = "Output columns to be calculated.")
    @JIPipeParameter("output-columns")
    public ParameterCollectionList getOutputColumns() {
        return outputColumns;
    }

    @JIPipeParameter("output-columns")
    public void setOutputColumns(ParameterCollectionList outputColumns) {
        this.outputColumns = outputColumns;
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

    public static class OutputColumn extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter outputColumnName = new JIPipeExpressionParameter("\"Output\"");
        private JIPipeExpressionParameter outputColumnValues = new JIPipeExpressionParameter("");

        public OutputColumn() {
        }

        public OutputColumn(JIPipeExpressionParameter outputColumnName, JIPipeExpressionParameter outputColumnValues) {
            this.outputColumnName = outputColumnName;
            this.outputColumnValues = outputColumnValues;
        }

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
        @AddJIPipeExpressionParameterVariable(name = "Bin limit (min)", key = "bin_limit_min", description = "The lower bin limit")
        @AddJIPipeExpressionParameterVariable(name = "Bin limit (max)", key = "bin_limit_max", description = "The upper bin limit")
        @AddJIPipeExpressionParameterVariable(name = "Bin values (unique)", key = "unique_values", description = "The list of unique values inside the bin")
        @AddJIPipeExpressionParameterVariable(name = "Bin count (raw)", key = "count", description = "The count of values inside the bin")
        @AddJIPipeExpressionParameterVariable(name = "Bin count (weighted)", key = "weighted_count", description = "The weighted count of values inside the bin")
        @AddJIPipeExpressionParameterVariable(name = "Bin count (raw, cumulative)", key = "cumulative_count", description = "The cumulative count of values inside the bin")
        @AddJIPipeExpressionParameterVariable(name = "Bin count (weighted, cumulative)", key = "cumulative_weighted_count", description = "The cumulative weighted count of values inside the bin")
        @AddJIPipeExpressionParameterVariable(name = "Maximum bin count (raw)", key = "max_count", description = "The maximum count of values inside over all bins")
        @AddJIPipeExpressionParameterVariable(name = "Maximum bin count (weighted)", key = "max_weighted_count", description = "The maximum weighted count of values over all bins")
        @AddJIPipeExpressionParameterVariable(name = "Maximum bin count (raw, cumulative)", key = "max_cumulative_count", description = "The maximum cumulative count of values over all bins")
        @AddJIPipeExpressionParameterVariable(name = "Maximum bin count (weighted, cumulative)", key = "max_cumulative_weighted_count", description = "The maximum cumulative weighted count of values over all bins")
        public JIPipeExpressionParameter getOutputColumnValues() {
            return outputColumnValues;
        }

        @JIPipeParameter("value")
        public void setOutputColumnValues(JIPipeExpressionParameter outputColumnValues) {
            this.outputColumnValues = outputColumnValues;
        }
    }

    public static class HistogramBin {
        private final double startBoundary;
        private final double endBoundary;
        private final TDoubleSet uniqueValues = new TDoubleHashSet();
        private final TDoubleList values = new TDoubleArrayList();
        private double weightedCount = 0;
        private double cumulativeWeightedCount = 0;
        private double cumulativeCount = 0;

        public HistogramBin(double startBoundary, double endBoundary) {
            this.startBoundary = startBoundary;
            this.endBoundary = endBoundary;
        }

        public double getStartBoundary() {
            return startBoundary;
        }

        public double getEndBoundary() {
            return endBoundary;
        }

        public void increment(double value, double weight) {
            uniqueValues.add(value);
            values.add(value);
            weightedCount += value * weight;
        }

        public TDoubleSet getUniqueValues() {
            return uniqueValues;
        }

        public TDoubleList getValues() {
            return values;
        }

        public double getWeightedCount() {
            return weightedCount;
        }

        public double getCount() {
            return values.size();
        }

        public double getCumulativeCount() {
            return cumulativeCount;
        }

        public void setCumulativeCount(double cumulativeCount) {
            this.cumulativeCount = cumulativeCount;
        }

        public double getCumulativeWeightedCount() {
            return cumulativeWeightedCount;
        }

        public void setCumulativeWeightedCount(double cumulativeWeightedCount) {
            this.cumulativeWeightedCount = cumulativeWeightedCount;
        }
    }
}
