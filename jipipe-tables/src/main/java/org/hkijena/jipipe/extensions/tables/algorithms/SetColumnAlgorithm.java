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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringOrDoublePairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringOrDouble;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@JIPipeDocumentation(name = "Set table column", description = "Adds a new column or replaces an existing table column by explicitly setting it to a value")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class SetColumnAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringAndStringOrDoublePairParameter.List columns = new StringAndStringOrDoublePairParameter.List();
    private boolean replaceIfExists = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SetColumnAlgorithm(JIPipeNodeInfo info) {
        super(info);
        columns.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SetColumnAlgorithm(SetColumnAlgorithm other) {
        super(other);
        this.replaceIfExists = other.replaceIfExists;
        this.columns = new StringAndStringOrDoublePairParameter.List(other.columns);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class).duplicate();
        for (StringAndStringOrDoublePairParameter entry : columns) {
            String columnName = entry.getKey();

            if (table.getColumnIndex(columnName) != -1 && !replaceIfExists)
                continue;

            int columnId = table.getOrCreateColumnIndex(columnName);
            StringOrDouble value = entry.getValue();

            if (value.getMode() == StringOrDouble.Mode.Double) {
                for (int row = 0; row < table.getRowCount(); ++row) {
                    table.getTable().setValue(columnId, row, value.getDoubleValue());
                }
            } else {
                for (int row = 0; row < table.getRowCount(); ++row) {
                    table.getTable().setValue(columnId, row, value.getStringValue());
                }
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), table);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (StringAndStringOrDoublePairParameter column : columns) {
            if (StringUtils.isNullOrEmpty(column.getKey())) {
                report.forCategory("Columns").reportIsInvalid("Column names cannot be empty!",
                        "It is not allowed that column names are empty.",
                        "Check if all column names are non-empty.",
                        this);
            }
        }
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
    @StringParameterSettings(monospace = true)
    @PairParameterSettings(singleRow = false, keyLabel = "Column name", valueLabel = "Column value")
    public StringAndStringOrDoublePairParameter.List getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(StringAndStringOrDoublePairParameter.List columns) {
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
        for (Class<? extends JIPipeData> klass : JIPipe.getDataTypes().getRegisteredDataTypes().values()) {
            if (TableColumn.isGeneratingTableColumn(klass)) {
                JIPipeDataInfo info = JIPipeDataInfo.getInstance(klass);
                markdown.append("<tr><td><strong>").append(HtmlEscapers.htmlEscaper().escape(info.getName())).append("</strong></td><td>")
                        .append(HtmlEscapers.htmlEscaper().escape(info.getDescription())).append("</td></tr>");
            }
        }

        markdown.append("</table>");
        result.setDescription(markdown.toString());

        return result;
    }
}
