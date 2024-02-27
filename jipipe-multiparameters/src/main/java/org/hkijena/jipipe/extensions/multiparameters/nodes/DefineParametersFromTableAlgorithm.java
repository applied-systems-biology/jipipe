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

package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.TableColumnSourceExpressionParameter;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeParameterTypeInfoRef;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Convert tables to parameters", description = "Converts a table into a parameter table. Please note that you have to define how each individual column is converted into a parameter. " +
        "To do this, add an entry into the 'Table columns' list and provide the parameter key, parameter type, and from which table column the value is sourced.")
@ConfigureJIPipeNode(menuPath = "Convert", nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Output", create = true)
public class DefineParametersFromTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList columns;

    public DefineParametersFromTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        columns = ParameterCollectionList.containingCollection(Column.class);
    }

    public DefineParametersFromTableAlgorithm(DefineParametersFromTableAlgorithm other) {
        super(other);
        columns = new ParameterCollectionList(other.columns);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ResultsTableData inputData = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        // Generate columns
        Map<String, List<Object>> valueMap = new HashMap<>();
        int nRow = 0;
        for (Column column : columns.mapToCollection(Column.class)) {
            TableColumn tableColumn = column.getTableColumn().pickOrGenerateColumn(inputData);
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < tableColumn.getRows(); i++) {
                values.add(parseColumnValue(tableColumn.getRowAsObject(i), column));
            }
            nRow = Math.max(nRow, values.size());
            valueMap.put(column.getKey(), values);
        }

        // Normalize columns
        for (List<Object> objects : valueMap.values()) {
            while (objects.size() < nRow) {
                objects.add(objects.get(objects.size() - 1));
            }
        }

        // Generate parameters and write
        for (int row = 0; row < nRow; ++row) {
            ParametersData data = new ParametersData();
            for (Map.Entry<String, List<Object>> entry : valueMap.entrySet()) {
                data.getParameterData().put(entry.getKey(), entry.getValue().get(row));
            }
            getFirstOutputSlot().addData(data, JIPipeDataContext.create(this), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Table columns", description = "For each parameter, add an entry that describes the generated parameter key/type and the source table column.")
    @JIPipeParameter("table-columns")
    public ParameterCollectionList getColumns() {
        return columns;
    }

    @JIPipeParameter("table-columns")
    public void setColumns(ParameterCollectionList columns) {
        this.columns = columns;
    }


    private Object parseColumnValue(Object obj, Column column) {
        if (column.valuesAreJson) {
            String json;
            if (obj instanceof String) {
                json = (String) obj;
            } else {
                json = JsonUtils.toJsonString(obj);
            }
            return JsonUtils.readFromString(json, column.type.getInfo().getFieldClass());
        } else {
            Class<?> fieldClass = column.getType().getInfo().getFieldClass();
            // Handle special case: numbers
            if (Number.class.isAssignableFrom(obj.getClass())) {
                if (fieldClass == Byte.class) {
                    return ((Number) obj).byteValue();
                } else if (fieldClass == Short.class) {
                    return ((Number) obj).shortValue();
                } else if (fieldClass == Integer.class) {
                    return ((Number) obj).intValue();
                } else if (fieldClass == Long.class) {
                    return ((Number) obj).longValue();
                } else if (fieldClass == Float.class) {
                    return ((Number) obj).floatValue();
                } else if (fieldClass == Double.class) {
                    return ((Number) obj).doubleValue();
                }
            }
            if (column.type.getInfo().getFieldClass().isAssignableFrom(obj.getClass())) {
                return obj;
            }
            throw new RuntimeException("Cannot convert " + obj + " into a value compatible with " + fieldClass);
        }
    }

    public static class Column extends AbstractJIPipeParameterCollection {

        private TableColumnSourceExpressionParameter tableColumn = new TableColumnSourceExpressionParameter();
        private String key;
        private JIPipeParameterTypeInfoRef type = new JIPipeParameterTypeInfoRef();
        private boolean valuesAreJson = false;

        public Column() {
        }

        public Column(Column other) {
            this.tableColumn = new TableColumnSourceExpressionParameter(other.tableColumn);
            this.key = other.key;
            this.type = new JIPipeParameterTypeInfoRef(other.type);
            this.valuesAreJson = other.valuesAreJson;
        }

        @SetJIPipeDocumentation(name = "Table column", description = "The table column")
        @JIPipeParameter("table-column")
        @ParameterCollectionListTemplate(Column.class)
        public TableColumnSourceExpressionParameter getTableColumn() {
            return tableColumn;
        }

        @JIPipeParameter("table-column")
        public void setTableColumn(TableColumnSourceExpressionParameter tableColumn) {
            this.tableColumn = tableColumn;
        }

        @SetJIPipeDocumentation(name = "Key", description = "The parameter key")
        @JIPipeParameter("key")
        @StringParameterSettings(monospace = true)
        public String getKey() {
            return key;
        }

        @JIPipeParameter("key")
        public void setKey(String key) {
            this.key = key;
        }

        @SetJIPipeDocumentation(name = "Type", description = "The parameter type")
        @JIPipeParameter("type")
        public JIPipeParameterTypeInfoRef getType() {
            return type;
        }

        @JIPipeParameter("type")
        public void setType(JIPipeParameterTypeInfoRef type) {
            this.type = type;
        }

        @SetJIPipeDocumentation(name = "Values are JSON", description = "Whether values are JSON")
        @JIPipeParameter("values-are-json")
        public boolean isValuesAreJson() {
            return valuesAreJson;
        }

        @JIPipeParameter("values-are-json")
        public void setValuesAreJson(boolean valuesAreJson) {
            this.valuesAreJson = valuesAreJson;
        }
    }
}
