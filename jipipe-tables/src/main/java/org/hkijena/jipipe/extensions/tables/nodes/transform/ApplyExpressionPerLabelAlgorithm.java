package org.hkijena.jipipe.extensions.tables.nodes.transform;

import com.google.common.primitives.Doubles;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TDoubleObjectHashMap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Apply expression per label", description = "Given a table with two numeric columns containing a key and a value, summarize all values assigned to a key into a single value. Allows the generation of normalized and cumulative histograms.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ApplyExpressionPerLabelAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter keyColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"key\"");
    private TableColumnSourceExpressionParameter valueColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"value\"");

    private String outputKeyColumn = "key";

    private String outputValueColumn = "value";
    private DefaultExpressionParameter integrationFunction = new DefaultExpressionParameter("SUM(values)");
    private boolean cumulative = false;
    private boolean normalize = false;

    public ApplyExpressionPerLabelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ApplyExpressionPerLabelAlgorithm(ApplyExpressionPerLabelAlgorithm other) {
        super(other);
        this.keyColumn = new TableColumnSourceExpressionParameter(other.keyColumn);
        this.valueColumn = new TableColumnSourceExpressionParameter(other.valueColumn);
        this.integrationFunction = new DefaultExpressionParameter(other.integrationFunction);
        this.cumulative = other.cumulative;
        this.normalize = other.normalize;
        this.outputKeyColumn = other.outputKeyColumn;
        this.outputValueColumn = other.outputValueColumn;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        TDoubleObjectHashMap<TDoubleList> bucketedValues = new TDoubleObjectHashMap<>();

        // Copy the buckets
        TableColumn keyColumn_ = keyColumn.pickOrGenerateColumn(inputTable);
        TableColumn valueColumn_ = valueColumn.pickOrGenerateColumn(inputTable);

        for (int row = 0; row < inputTable.getRowCount(); row++) {
            double key = keyColumn_.getRowAsDouble(row);
            double value = valueColumn_.getRowAsDouble(row);
            TDoubleList list = bucketedValues.get(key);
            if (list == null) {
                list = new TDoubleArrayList();
                bucketedValues.put(key, list);
            }
            list.add(value);
        }

        // Setup values
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        // Integrate buckets
        TDoubleDoubleMap integratedValues = new TDoubleDoubleHashMap();
        for (double key : bucketedValues.keys()) {
            TDoubleList list = bucketedValues.get(key);
            List<Double> asList = Doubles.asList(list.toArray());
            variables.set("values", asList);
            double integrated = integrationFunction.evaluateToDouble(variables);
            integratedValues.put(key, integrated);
        }

        // Get sorted keys
        double[] sortedKeys = integratedValues.keys();
        Arrays.sort(sortedKeys);

        // Cumulative if enabled
        if (cumulative) {
            TDoubleDoubleMap cumulativeIntegratedValues = new TDoubleDoubleHashMap();
            double cumulativeValue = 0;
            for (double key : sortedKeys) {
                double value = integratedValues.get(key);
                double value_ = value + cumulativeValue;
                cumulativeValue += value;
                cumulativeIntegratedValues.put(key, value_);
            }
            integratedValues = cumulativeIntegratedValues;
        }

        // Normalize if enabled
        if (normalize) {
            double max = Double.NEGATIVE_INFINITY;
            for (double value : integratedValues.values()) {
                max = Math.max(max, value);
            }
            for (double key : integratedValues.keys()) {
                integratedValues.put(key, integratedValues.get(key) / max);
            }
        }

        // Convert to table
        ResultsTableData outputTable = new ResultsTableData();
        int outputKeyColumnIndex = outputTable.addNumericColumn(outputKeyColumn);
        int outputValueColumnIndex = outputTable.addNumericColumn(outputValueColumn);
        for (double key : sortedKeys) {
            double value = integratedValues.get(key);
            int row = outputTable.addRow();
            outputTable.setValueAt(key, row, outputKeyColumnIndex);
            outputTable.setValueAt(value, row, outputValueColumnIndex);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @JIPipeDocumentation(name = "Output column (keys)", description = "The table column where the keys will be written to")
    @JIPipeParameter(value = "output-key-column", uiOrder = 100)
    @StringParameterSettings(monospace = true)
    public String getOutputKeyColumn() {
        return outputKeyColumn;
    }

    @JIPipeParameter("output-key-column")
    public void setOutputKeyColumn(String outputKeyColumn) {
        this.outputKeyColumn = outputKeyColumn;
    }

    @JIPipeDocumentation(name = "Output column (integrated values)", description = "The table column where the integrated values will be written to")
    @JIPipeParameter(value = "output-value-column", uiOrder = 110)
    @StringParameterSettings(monospace = true)
    public String getOutputValueColumn() {
        return outputValueColumn;
    }

    @JIPipeParameter("output-value-column")
    public void setOutputValueColumn(String outputValueColumn) {
        this.outputValueColumn = outputValueColumn;
    }

    @JIPipeDocumentation(name = "Key column", description = "The column that contains the key values")
    @JIPipeParameter(value = "key-column", important = true, uiOrder = -100)
    public TableColumnSourceExpressionParameter getKeyColumn() {
        return keyColumn;
    }

    @JIPipeParameter("key-column")
    public void setKeyColumn(TableColumnSourceExpressionParameter keyColumn) {
        this.keyColumn = keyColumn;
    }

    @JIPipeDocumentation(name = "Value column", description = "The column that contains the values")
    @JIPipeParameter(value = "value-column", important = true, uiOrder = -90)
    public TableColumnSourceExpressionParameter getValueColumn() {
        return valueColumn;
    }

    @JIPipeParameter("value-column")
    public void setValueColumn(TableColumnSourceExpressionParameter valueColumn) {
        this.valueColumn = valueColumn;
    }

    @JIPipeDocumentation(name = "Summary function", description = "The function that summarizes the values assigned to the same key")
    @JIPipeParameter("integration-function")
    @ExpressionParameterSettingsVariable(key = "values", name = "Values", description = "The values to be integrated")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getIntegrationFunction() {
        return integrationFunction;
    }

    @JIPipeParameter("integration-function")
    public void setIntegrationFunction(DefaultExpressionParameter integrationFunction) {
        this.integrationFunction = integrationFunction;
    }

    @JIPipeDocumentation(name = "Cumulative", description = "If enabled, the histogram will be cumulative")
    @JIPipeParameter("cumulative")
    public boolean isCumulative() {
        return cumulative;
    }

    @JIPipeParameter("cumulative")
    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
    }

    @JIPipeDocumentation(name = "Normalize", description = "If enabled, normalizes the values")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }
}
