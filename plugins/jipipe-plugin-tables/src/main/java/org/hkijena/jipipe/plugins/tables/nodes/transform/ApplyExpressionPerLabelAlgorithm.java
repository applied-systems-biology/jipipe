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
import gnu.trove.map.hash.TDoubleObjectHashMap;
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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Apply expression per label", description = "Given a table with two numeric columns containing a key and a value, summarize all values assigned to a key into a single value. Allows the generation of normalized and cumulative histograms.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Transform")
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ApplyExpressionPerLabelAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnSourceExpressionParameter keyColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"key\"");
    private TableColumnSourceExpressionParameter valueColumn = new TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType.ExistingColumn, "\"value\"");

    private String outputKeyColumn = "key";

    private String outputValueColumn = "value";
    private JIPipeExpressionParameter integrationFunction = new JIPipeExpressionParameter("SUM(values)");
    private boolean cumulative = false;
    private boolean normalize = false;

    public ApplyExpressionPerLabelAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ApplyExpressionPerLabelAlgorithm(ApplyExpressionPerLabelAlgorithm other) {
        super(other);
        this.keyColumn = new TableColumnSourceExpressionParameter(other.keyColumn);
        this.valueColumn = new TableColumnSourceExpressionParameter(other.valueColumn);
        this.integrationFunction = new JIPipeExpressionParameter(other.integrationFunction);
        this.cumulative = other.cumulative;
        this.normalize = other.normalize;
        this.outputKeyColumn = other.outputKeyColumn;
        this.outputValueColumn = other.outputValueColumn;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData inputTable = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        TDoubleObjectHashMap<TDoubleList> bucketedValues = new TDoubleObjectHashMap<>();

        // Copy the buckets
        TableColumnData keyColumn_ = keyColumn.pickOrGenerateColumn(inputTable, new JIPipeExpressionVariablesMap(iterationStep));
        TableColumnData valueColumn_ = valueColumn.pickOrGenerateColumn(inputTable, new JIPipeExpressionVariablesMap(iterationStep));

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
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

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

        iterationStep.addOutputData(getFirstOutputSlot(), outputTable, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Output column (keys)", description = "The table column where the keys will be written to")
    @JIPipeParameter(value = "output-key-column", uiOrder = 100)
    @StringParameterSettings(monospace = true)
    public String getOutputKeyColumn() {
        return outputKeyColumn;
    }

    @JIPipeParameter("output-key-column")
    public void setOutputKeyColumn(String outputKeyColumn) {
        this.outputKeyColumn = outputKeyColumn;
    }

    @SetJIPipeDocumentation(name = "Output column (integrated values)", description = "The table column where the integrated values will be written to")
    @JIPipeParameter(value = "output-value-column", uiOrder = 110)
    @StringParameterSettings(monospace = true)
    public String getOutputValueColumn() {
        return outputValueColumn;
    }

    @JIPipeParameter("output-value-column")
    public void setOutputValueColumn(String outputValueColumn) {
        this.outputValueColumn = outputValueColumn;
    }

    @SetJIPipeDocumentation(name = "Key column", description = "The column that contains the key values")
    @JIPipeParameter(value = "key-column", important = true, uiOrder = -100)
    public TableColumnSourceExpressionParameter getKeyColumn() {
        return keyColumn;
    }

    @JIPipeParameter("key-column")
    public void setKeyColumn(TableColumnSourceExpressionParameter keyColumn) {
        this.keyColumn = keyColumn;
    }

    @SetJIPipeDocumentation(name = "Value column", description = "The column that contains the values")
    @JIPipeParameter(value = "value-column", important = true, uiOrder = -90)
    public TableColumnSourceExpressionParameter getValueColumn() {
        return valueColumn;
    }

    @JIPipeParameter("value-column")
    public void setValueColumn(TableColumnSourceExpressionParameter valueColumn) {
        this.valueColumn = valueColumn;
    }

    @SetJIPipeDocumentation(name = "Summary function", description = "The function that summarizes the values assigned to the same key")
    @JIPipeParameter("integration-function")
    @AddJIPipeExpressionParameterVariable(key = "values", name = "Values", description = "The values to be integrated")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getIntegrationFunction() {
        return integrationFunction;
    }

    @JIPipeParameter("integration-function")
    public void setIntegrationFunction(JIPipeExpressionParameter integrationFunction) {
        this.integrationFunction = integrationFunction;
    }

    @SetJIPipeDocumentation(name = "Cumulative", description = "If enabled, the histogram will be cumulative")
    @JIPipeParameter("cumulative")
    public boolean isCumulative() {
        return cumulative;
    }

    @JIPipeParameter("cumulative")
    public void setCumulative(boolean cumulative) {
        this.cumulative = cumulative;
    }

    @SetJIPipeDocumentation(name = "Normalize", description = "If enabled, normalizes the values")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }
}
