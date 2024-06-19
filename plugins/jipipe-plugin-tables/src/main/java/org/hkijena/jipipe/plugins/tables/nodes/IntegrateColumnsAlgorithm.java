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

package org.hkijena.jipipe.plugins.tables.nodes;

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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.tables.ColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.RelabeledTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.plugins.tables.parameters.collections.IntegratingTableColumnProcessorParameterList;
import org.hkijena.jipipe.plugins.tables.parameters.processors.IntegratingTableColumnProcessorParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Summarize table", description = "Summarize table columns by applying predefined operations like average, standard deviation, or median.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
public class IntegrateColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntegratingTableColumnProcessorParameterList processorParameters = new IntegratingTableColumnProcessorParameterList();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public IntegrateColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        processorParameters.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegrateColumnsAlgorithm(IntegrateColumnsAlgorithm other) {
        super(other);
        this.processorParameters = new IntegratingTableColumnProcessorParameterList(other.processorParameters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<TableColumn> resultColumns = new ArrayList<>();
        for (IntegratingTableColumnProcessorParameter processor : processorParameters) {
            String sourceColumn = processor.getInput().queryFirst(input.getColumnNames(), new JIPipeExpressionVariablesMap());
            if (sourceColumn == null) {
                throw new JIPipeValidationRuntimeException(new NullPointerException("Could not find column matching '" + processor.getInput() + "'"),
                        "Unable to find column matching " + processor.getInput(),
                        "The column filter '" + processor.getInput() + "' tried to find a matching column in " + String.join(", ", input.getColumnNames()) + ". None of the columns matched.",
                        "Please check if the filter is correct.");
            }
            TableColumn sourceColumnData = input.getColumnReference(input.getColumnIndex(sourceColumn));
            ColumnOperation columnOperation = ((JIPipeExpressionRegistry.ColumnOperationEntry) processor.getParameter().getValue()).getOperation();
            TableColumn resultColumn = columnOperation.apply(sourceColumnData);
            resultColumns.add(new RelabeledTableColumn(resultColumn, processor.getOutput()));
        }

        // Combine into one table
        ResultsTableData output = new ResultsTableData(resultColumns);
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        report.report(new ParameterValidationReportContext(reportContext, this, "Processors", "processors"), processorParameters);
    }

    @SetJIPipeDocumentation(name = "Processors", description = "Defines which columns are processed")
    @JIPipeParameter("processors")
    public IntegratingTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @JIPipeParameter("processors")
    public void setProcessorParameters(IntegratingTableColumnProcessorParameterList processorParameters) {
        this.processorParameters = processorParameters;
    }
}
