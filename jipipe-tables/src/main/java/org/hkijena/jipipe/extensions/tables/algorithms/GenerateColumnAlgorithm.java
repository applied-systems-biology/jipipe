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

import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDeclaration;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.tables.ColumnContentType;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.collections.TableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.enums.TableColumnGeneratorParameter;
import org.hkijena.jipipe.extensions.tables.parameters.processors.TableColumnGeneratorProcessor;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@JIPipeDocumentation(name = "Generate table column", description = "Adds a new column or replaces an existing table column by generating values")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class GenerateColumnAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private TableColumnGeneratorProcessorParameterList columns = new TableColumnGeneratorProcessorParameterList();
    private boolean replaceIfExists = false;

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public GenerateColumnAlgorithm(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
        columns.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GenerateColumnAlgorithm(GenerateColumnAlgorithm other) {
        super(other);
        this.replaceIfExists = other.replaceIfExists;
        this.columns = new TableColumnGeneratorProcessorParameterList(other.columns);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = (ResultsTableData) dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class).duplicate();
        for (TableColumnGeneratorProcessor entry : columns) {
            String columnName = entry.getValue();

            if (table.getColumnIndex(columnName) != -1 && !replaceIfExists)
                continue;

            TableColumnGeneratorParameter generatorParameter = entry.getKey();
            TableColumn generator = (TableColumn) JIPipeData.createInstance(generatorParameter.getGeneratorType().getDeclaration().getDataClass());
            int columnId = table.getOrCreateColumnIndex(columnName);

            if (generatorParameter.getGeneratedType() == ColumnContentType.NumericColumn) {
                double[] data = generator.getDataAsDouble(table.getRowCount());
                for (int row = 0; row < table.getRowCount(); ++row) {
                    table.getTable().setValue(columnId, row, data[row]);
                }
            } else {
                String[] data = generator.getDataAsString(table.getRowCount());
                for (int row = 0; row < table.getRowCount(); ++row) {
                    table.getTable().setValue(columnId, row, data[row]);
                }
            }
        }
        dataInterface.addOutputData(getFirstOutputSlot(), table);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Columns").report(columns);
    }

    @JIPipeDocumentation(name = "Replace existing data", description = "If the target column exists, replace its content")
    @JIPipeParameter("replace-existing")
    public boolean isReplaceIfExists() {
        return replaceIfExists;
    }

    @JIPipeParameter("replace-existing")
    public void setReplaceIfExists(boolean replaceIfExists) {
        this.replaceIfExists = replaceIfExists;
    }

    @JIPipeDocumentation(name = "Columns", description = "Columns to be generated")
    @JIPipeParameter("columns")
    public TableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(TableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
    }

    /**
     * Used for generating a new generator entry
     *
     * @param definition the parameter definition
     * @return generated parameter
     */
    public static JIPipeMutableParameterAccess generateColumnParameter(JIPipeDynamicParameterCollection.UserParameterDefinition definition) {
        JIPipeMutableParameterAccess result = new JIPipeMutableParameterAccess(definition.getSource(), definition.getName(), definition.getFieldClass());
        result.setKey(definition.getName());

        StringBuilder markdown = new StringBuilder();
        markdown.append("You can select from one of the following generators: ");
        markdown.append("<table>");
        for (Class<? extends JIPipeData> klass : JIPipeDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                JIPipeDataDeclaration declaration = JIPipeDataDeclaration.getInstance(klass);
                markdown.append("<tr><td><strong>").append(HtmlEscapers.htmlEscaper().escape(declaration.getName())).append("</strong></td><td>")
                        .append(HtmlEscapers.htmlEscaper().escape(declaration.getDescription())).append("</td></tr>");
            }
        }

        markdown.append("</table>");
        result.setDescription(markdown.toString());

        return result;
    }
}
