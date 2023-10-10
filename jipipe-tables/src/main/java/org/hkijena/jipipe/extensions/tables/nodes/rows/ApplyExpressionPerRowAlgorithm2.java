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
 *
 */

package org.hkijena.jipipe.extensions.tables.nodes.rows;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.*;

@JIPipeDocumentation(name = "Apply expression per row", description = "Applies an expression for each row. The column values are available as variables.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ApplyExpressionPerRowAlgorithm2 extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customExpressionVariables;
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);

    public ApplyExpressionPerRowAlgorithm2(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
        entries.addNewInstance();
    }

    public ApplyExpressionPerRowAlgorithm2(ApplyExpressionPerRowAlgorithm2 other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.entries = new ParameterCollectionList(other.entries);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        ExpressionVariables variableSet = new ExpressionVariables();
        Map<String, String> annotationsMap = JIPipeTextAnnotation.annotationListToMap(dataBatch.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        variableSet.set("annotations", annotationsMap);
        customExpressionVariables.writeToVariables(variableSet, true, "custom.", true, "custom");
        variableSet.set("num_rows", data.getRowCount());
        for (int col = 0; col < data.getColumnCount(); col++) {
            TableColumn column = data.getColumnReference(col);
            if (column.isNumeric()) {
                variableSet.set("all." + column.getLabel(), Doubles.asList(column.getDataAsDouble(column.getRows())));
            } else {
                variableSet.set("all." + column.getLabel(), Arrays.asList(column.getDataAsString(column.getRows())));
            }
        }
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            List<Object> generatedValues = new ArrayList<>();
            variableSet.set("num_cols", data.getColumnCount());

            // Create output column
            String columnName = entry.getColumnName().evaluateToString(variableSet);
            int columnIndex = data.getColumnIndex(columnName);

            variableSet.set("column", columnIndex);
            variableSet.set("column_name", columnName);

            for (int row = 0; row < data.getRowCount(); row++) {
                variableSet.set("row", row);
                for (int col = 0; col < data.getColumnCount(); col++) {
                    variableSet.set(data.getColumnName(col), data.getValueAt(row, col));
                }
                generatedValues.add(entry.getValue().evaluate(variableSet));
            }
            boolean numeric = generatedValues.stream().allMatch(o -> o instanceof Number);
            int targetColumn = data.getOrCreateColumnIndex(columnName, !numeric);
            for (int row = 0; row < data.getRowCount(); row++) {
                data.setValueAt(generatedValues.get(row), row, targetColumn);
            }
        }
        dataBatch.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated values", description = "List of expressions that describe how new values are generated")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-expression-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {
        private final static Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new ExpressionParameterVariable("<Column>", "Value of the column in the current row", ""));
            VARIABLES.add(new ExpressionParameterVariable("all.<Column>", "Values of all columns", ""));
            VARIABLES.add(new ExpressionParameterVariable("Table column index", "The column index", "column"));
            VARIABLES.add(new ExpressionParameterVariable("Table column name", "The column name", "column_name"));
            VARIABLES.add(new ExpressionParameterVariable("Number of rows", "The number of rows within the table", "num_rows"));
            VARIABLES.add(new ExpressionParameterVariable("Number of columns", "The number of columns within the table", "num_cols"));
            VARIABLES.add(new ExpressionParameterVariable("Annotations", "Map of annotations of the current data batch", "annotations"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private DefaultExpressionParameter columnName = new DefaultExpressionParameter("\"Output column name\"");
        private DefaultExpressionParameter value = new DefaultExpressionParameter("row");

        public Entry() {
        }

        public Entry(Entry other) {
            this.columnName = new DefaultExpressionParameter(other.columnName);
            this.value = new DefaultExpressionParameter(other.value);
        }

        @JIPipeDocumentation(name = "Column name", description = "The name of the column where the value will be written")
        @JIPipeParameter("column-name")
        @ExpressionParameterSettingsVariable(fromClass = VariableSource.class)
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        public DefaultExpressionParameter getColumnName() {
            return columnName;
        }

        @JIPipeParameter("column-name")
        public void setColumnName(DefaultExpressionParameter columnName) {
            this.columnName = columnName;
        }

        @JIPipeDocumentation(name = "Value", description = "The generated value")
        @JIPipeParameter("value")
        @ExpressionParameterSettingsVariable(fromClass = VariableSource.class)
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @ExpressionParameterSettingsVariable(name = "Column name", description = "The output column name", key = "column_name")
        @ExpressionParameterSettingsVariable(name = "Column index", description = "The output column index", key = "column")
        public DefaultExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(DefaultExpressionParameter value) {
            this.value = value;
        }
    }
}
