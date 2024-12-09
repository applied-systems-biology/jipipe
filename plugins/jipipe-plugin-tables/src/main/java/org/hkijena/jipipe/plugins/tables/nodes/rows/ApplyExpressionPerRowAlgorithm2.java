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

package org.hkijena.jipipe.plugins.tables.nodes.rows;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;

import java.util.*;

@SetJIPipeDocumentation(name = "Apply expression per row", description = "Applies an expression for each row. The column values are available as variables.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class ApplyExpressionPerRowAlgorithm2 extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);

    public ApplyExpressionPerRowAlgorithm2(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public ApplyExpressionPerRowAlgorithm2(ApplyExpressionPerRowAlgorithm2 other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData data = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        Map<String, String> annotationsMap = JIPipeTextAnnotation.annotationListToMap(iterationStep.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        variableSet.set("annotations", annotationsMap);
        getDefaultCustomExpressionVariables().writeToVariables(variableSet);
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
        iterationStep.addOutputData(getFirstOutputSlot(), data, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generated values", description = "List of expressions that describe how new values are generated")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {
        private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("", "<Column>", "Value of the column in the current row"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("", "all.<Column>", "Values of all columns"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("column", "Table column index", "The column index"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("column_name", "Table column name", "The column name"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_rows", "Number of rows", "The number of rows within the table"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_cols", "Number of columns", "The number of columns within the table"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Annotations", "Map of annotations of the current iteration step"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter columnName = new JIPipeExpressionParameter("\"Output column name\"");
        private JIPipeExpressionParameter value = new JIPipeExpressionParameter("row");

        public Entry() {
        }

        public Entry(Entry other) {
            this.columnName = new JIPipeExpressionParameter(other.columnName);
            this.value = new JIPipeExpressionParameter(other.value);
        }

        @SetJIPipeDocumentation(name = "Column name", description = "The name of the column where the value will be written")
        @JIPipeParameter("column-name")
        @AddJIPipeExpressionParameterVariable(fromClass = VariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @AddJIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        public JIPipeExpressionParameter getColumnName() {
            return columnName;
        }

        @JIPipeParameter("column-name")
        public void setColumnName(JIPipeExpressionParameter columnName) {
            this.columnName = columnName;
        }

        @SetJIPipeDocumentation(name = "Value", description = "The generated value")
        @JIPipeParameter("value")
        @AddJIPipeExpressionParameterVariable(fromClass = VariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @AddJIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        @AddJIPipeExpressionParameterVariable(name = "Column name", description = "The output column name", key = "column_name")
        @AddJIPipeExpressionParameterVariable(name = "Column index", description = "The output column index", key = "column")
        public JIPipeExpressionParameter getValue() {
            return value;
        }

        @JIPipeParameter("value")
        public void setValue(JIPipeExpressionParameter value) {
            this.value = value;
        }
    }
}
