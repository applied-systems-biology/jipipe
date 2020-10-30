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
 */

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Split table by columns", description = "Splits a table into multiple tables according to list of selected columns. " +
        "Sub-tables that have the same values in the selected columns are put into the same output table.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Split")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class SplitTableByColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalStringParameter generatedAnnotation = new OptionalStringParameter();
    private StringQueryExpression columns = new StringQueryExpression();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SplitTableByColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        generatedAnnotation.setContent("Row filter");
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableByColumnsAlgorithm(SplitTableByColumnsAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.columns = new StringQueryExpression(other.columns);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class);
        List<String> interestingColumns = columns.queryAll(input.getColumnNames());
        if (interestingColumns.isEmpty()) {
            dataBatch.addOutputData(getFirstOutputSlot(), input.duplicate());
        } else {
            List<String> rowConditions = new ArrayList<>();
            for (int row = 0; row < input.getRowCount(); row++) {
                int finalRow = row;
                rowConditions.add(interestingColumns.stream().map(col -> col + "=" + input.getValueAsString(finalRow, col)).collect(Collectors.joining(";")));
            }
            List<Integer> rows = new ArrayList<>(input.getRowCount());
            for (int i = 0; i < input.getRowCount(); i++) {
                rows.add(i);
            }
            Map<String, List<Integer>> groupedByCondition = rows.stream().collect(Collectors.groupingBy(rowConditions::get));
            for (Map.Entry<String, List<Integer>> entry : groupedByCondition.entrySet()) {
                ResultsTableData output = input.getRows(entry.getValue());
                List<JIPipeAnnotation> traits = new ArrayList<>();
                if (generatedAnnotation.isEnabled() && !StringUtils.isNullOrEmpty(generatedAnnotation.getContent())) {
                    traits.add(new JIPipeAnnotation(generatedAnnotation.getContent(), entry.getKey()));
                }
                dataBatch.addOutputData(getFirstOutputSlot(), output, traits);
            }
        }
    }

    @JIPipeDocumentation(name = "Generated annotation", description = "Optional. The annotation that is created for each table column. The column header will be stored inside it.")
    @JIPipeParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public OptionalStringParameter getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @JIPipeParameter("generated-annotation")
    public void setGeneratedAnnotation(OptionalStringParameter generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @JIPipeDocumentation(name = "Selected columns", description = "Expression that selects the columns. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("columns")
    public StringQueryExpression getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(StringQueryExpression columns) {
        this.columns = columns;
    }
}
