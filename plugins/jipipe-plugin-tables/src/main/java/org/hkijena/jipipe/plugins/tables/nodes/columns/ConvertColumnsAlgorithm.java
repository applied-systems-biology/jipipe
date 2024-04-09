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
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.tables.ColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.plugins.tables.parameters.collections.ConvertingTableColumnProcessorParameterList;
import org.hkijena.jipipe.plugins.tables.parameters.processors.ConvertingTableColumnProcessorParameter;

import java.util.HashMap;
import java.util.Map;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Apply function to columns", description = "Applies a converting function to all column values. " +
        "The result of the operation is stored in the same or a new column.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class ConvertColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ConvertingTableColumnProcessorParameterList processorParameters = new ConvertingTableColumnProcessorParameterList();
    private boolean append = true;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ConvertColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        processorParameters.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ConvertColumnsAlgorithm(ConvertColumnsAlgorithm other) {
        super(other);
        this.processorParameters = new ConvertingTableColumnProcessorParameterList(other.processorParameters);
        this.append = other.append;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        Map<String, TableColumn> resultColumns = new HashMap<>();
        for (ConvertingTableColumnProcessorParameter processor : processorParameters) {
            String sourceColumn = processor.getInput().queryFirst(input.getColumnNames(), new JIPipeExpressionVariablesMap());
            if (sourceColumn == null) {
                throw new JIPipeValidationRuntimeException(new NullPointerException("Unable to find column matching " + processor.getInput()),
                        "Unable to find column matching " + processor.getInput(),
                        "The column filter '" + processor.getInput() + "' tried to find a matching column in " + String.join(", ", input.getColumnNames()) + ". None of the columns matched.",
                        "Please check if the filter is correct.");
            }
            TableColumn sourceColumnData = input.getColumnReference(input.getColumnIndex(sourceColumn));
            ColumnOperation columnOperation = ((JIPipeExpressionRegistry.ColumnOperationEntry) processor.getParameter().getValue()).getOperation();
            TableColumn resultColumn = columnOperation.apply(sourceColumnData);
            resultColumns.put(processor.getOutput(), resultColumn);
        }
        if (append) {
            for (String columnName : input.getColumnNames()) {
                if (!resultColumns.containsKey(columnName)) {
                    resultColumns.put(columnName, input.getColumnReference(input.getColumnIndex(columnName)));
                }
            }
        }

        // Combine into one table
        ResultsTableData output = new ResultsTableData(resultColumns);
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Processors", description = "Defines which columns are processed")
    @JIPipeParameter("processors")
    public ConvertingTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @JIPipeParameter("processors")
    public void setProcessorParameters(ConvertingTableColumnProcessorParameterList processorParameters) {
        this.processorParameters = processorParameters;
    }

    @SetJIPipeDocumentation(name = "Append to existing table", description = "If enabled, the converted columns are appended to the existing table. Existing columns are overwritten.")
    @JIPipeParameter("append")
    public boolean isAppend() {
        return append;
    }

    @JIPipeParameter("append")
    public void setAppend(boolean append) {
        this.append = append;
    }
}
