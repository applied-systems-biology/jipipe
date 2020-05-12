package org.hkijena.acaq5.extensions.tables.algorithms;

import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQMutableParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.tables.ColumnContentType;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.parameters.TableColumnGeneratorParameter;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@ACAQDocumentation(name = "Generate table column", description = "Adds a new column or replaces an existing table column by generating values")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class GenerateColumnAlgorithm extends ACAQIteratingAlgorithm {

    private ACAQDynamicParameterCollection columns = new ACAQDynamicParameterCollection(true, TableColumnGeneratorParameter.class);
    private boolean replaceIfExists = false;

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public GenerateColumnAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        columns.setInstanceGenerator(GenerateColumnAlgorithm::generateColumnParameter);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GenerateColumnAlgorithm(GenerateColumnAlgorithm other) {
        super(other);
        this.replaceIfExists = other.replaceIfExists;
        this.columns = new ACAQDynamicParameterCollection(other.columns);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = (ResultsTableData) dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class).duplicate();
        for (Map.Entry<String, ACAQParameterAccess> entry : columns.getParameters().entrySet()) {
            String columnName = entry.getKey();

            if (table.getColumnIndex(columnName) != -1 && !replaceIfExists)
                continue;

            TableColumnGeneratorParameter generatorParameter = entry.getValue().get();
            TableColumn generator = (TableColumn) ACAQData.createInstance(generatorParameter.getGeneratorType().getDeclaration().getDataClass());
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
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Columns").report(columns);
    }

    @ACAQDocumentation(name = "Replace existing data", description = "If the target column exists, replace its content")
    @ACAQParameter("replace-existing")
    public boolean isReplaceIfExists() {
        return replaceIfExists;
    }

    @ACAQParameter("replace-existing")
    public void setReplaceIfExists(boolean replaceIfExists) {
        this.replaceIfExists = replaceIfExists;
    }

    @ACAQDocumentation(name = "Columns", description = "You can add as many columns as you want by clicking the '+' button. Then, select a generator and which " +
            "data type the column should have.")
    @ACAQParameter("columns")
    public ACAQDynamicParameterCollection getColumns() {
        return columns;
    }

    /**
     * Used for generating a new generator entry
     *
     * @param definition the parameter definition
     * @return generated parameter
     */
    public static ACAQMutableParameterAccess generateColumnParameter(ACAQDynamicParameterCollection.UserParameterDefinition definition) {
        ACAQMutableParameterAccess result = new ACAQMutableParameterAccess(definition.getSource(), definition.getName(), definition.getFieldClass());
        result.setKey(definition.getName());

        StringBuilder markdown = new StringBuilder();
        markdown.append("You can select from one of the following generators: ");
        markdown.append("<table>");
        for (Class<? extends ACAQData> klass : ACAQDatatypeRegistry.getInstance().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                ACAQDataDeclaration declaration = ACAQDataDeclaration.getInstance(klass);
                markdown.append("<tr><td><strong>").append(HtmlEscapers.htmlEscaper().escape(declaration.getName())).append("</strong></td><td>")
                        .append(HtmlEscapers.htmlEscaper().escape(declaration.getDescription())).append("</td></tr>");
            }
        }

        markdown.append("</table>");
        result.setDescription(markdown.toString());

        return result;
    }
}
