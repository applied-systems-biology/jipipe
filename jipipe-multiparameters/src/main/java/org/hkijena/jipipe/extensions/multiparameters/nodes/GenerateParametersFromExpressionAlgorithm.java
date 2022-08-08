package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.CustomExpressionVariablesParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.references.JIPipeParameterTypeInfoRef;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.*;

@JIPipeDocumentation(name = "Generate parameters from expression", description = "Generates a table of parameters from expressions defined in the 'Generated parameter columns' parameter.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Annotations", description = "Optional data that act as source for annotations.", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", description = "Generated parameters", autoCreate = true)
@JIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class GenerateParametersFromExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList columns;
    private final CustomExpressionVariablesParameter customVariables;

    public GenerateParametersFromExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        columns = ParameterCollectionList.containingCollection(Column.class);
        columns.addNewInstance();
        customVariables = new CustomExpressionVariablesParameter(this);
    }

    public GenerateParametersFromExpressionAlgorithm(GenerateParametersFromExpressionAlgorithm other) {
        super(other);
        columns = new ParameterCollectionList(other.columns);
        customVariables = new CustomExpressionVariablesParameter(other.customVariables, this);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        for (Column column : columns.mapToCollection(Column.class)) {
            if(StringUtils.isNullOrEmpty(column.key)) {
                report.reportIsInvalid("Column key cannot be empty!", "You cannot have empty parameter keys!", "Provide an appropriate parameter key.", column);
            }
            if(column.type.getInfo() == null) {
                report.reportIsInvalid("Column type cannot be empty!", "You cannot have empty parameter type!", "Provide an appropriate parameter type.", column);
            }
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        // Generate variables
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customVariables.writeToVariables(variables, true, "custom.", true, "custom");

        // Generate columns
        Map<String, List<Object>> valueMap = new HashMap<>();
        int nRow = 0;
        for (Column column : columns.mapToCollection(Column.class)) {
            Object evaluationResult = column.values.evaluate(variables);
            List<Object> values = new ArrayList<>();
            if(evaluationResult instanceof Collection) {
                for (Object o : ((Collection<?>) evaluationResult)) {
                    values.add(parseColumnValue(o, column));
                }
            }
            else {
                values.add(parseColumnValue(evaluationResult, column));
            }
            nRow = Math.max(nRow, values.size());
            valueMap.put(column.getKey(), values);
        }

        // Normalize columns
        for (List<Object> objects : valueMap.values()) {
            while(objects.size() < nRow) {
                objects.add(objects.get(objects.size() - 1));
            }
        }

        // Generate parameters and write
        for (int row = 0; row < nRow; ++row) {
            ParametersData data = new ParametersData();
            for (Map.Entry<String, List<Object>> entry : valueMap.entrySet()) {
                data.getParameterData().put(entry.getKey(), entry.getValue().get(row));
            }
            getFirstOutputSlot().addData(data, progressInfo);
        }
    }

    private Object parseColumnValue(Object obj, Column column) {
        if(column.valuesAreJson) {
            String json;
            if(obj instanceof String) {
                json = (String) obj;
            }
            else {
                json = JsonUtils.toJsonString(obj);
            }
            return JsonUtils.readFromString(json, column.type.getInfo().getFieldClass());
        }
        else {
            Class<?> fieldClass = column.getType().getInfo().getFieldClass();
            // Handle special case: numbers
            if(Number.class.isAssignableFrom(obj.getClass())) {
                if(fieldClass == Byte.class) {
                    return ((Number)obj).byteValue();
                }
                else if(fieldClass == Short.class) {
                    return ((Number)obj).shortValue();
                }
                else if(fieldClass == Integer.class) {
                    return ((Number)obj).intValue();
                }
                else if(fieldClass == Long.class) {
                    return ((Number)obj).longValue();
                }
                else if(fieldClass == Float.class) {
                    return ((Number)obj).floatValue();
                }
                else if(fieldClass == Double.class) {
                    return ((Number)obj).doubleValue();
                }
            }
            if(column.type.getInfo().getFieldClass().isAssignableFrom(obj.getClass())) {
                return obj;
            }
            throw new RuntimeException("Cannot convert " + obj + " into a value compatible with " + fieldClass);
        }
    }

    @JIPipeDocumentation(name = "Custom variables", description = "Here you can add parameters that will be included into the expressions as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomVariables() {
        return customVariables;
    }

    @JIPipeDocumentation(name = "Generated parameter columns", description = "Each item within this list defines a column in the generated parameter table. You must set the following properties: " +
            "<ul>" +
            "<li>Key: the internal key of the parameter that is referred to. You can look it up by clicking the (?) icon next to a parameter and reading the technical info in the documentation that will be shown.</li>" +
            "<li>Type: the data type of the parameter. Please ensure to select the correct type.</li>" +
            "<li>Values: an expression that should generate a string or an array/list of values. The expected value(s) are dependent on the 'Values are JSON' parameter.</li>" +
            "<li>Values are JSON: determines whether the values from the 'Values' parameter are in JSON format or already of the expected object type. If 'Values are JSON' is enabled and non-string values are returned (e.g., a map), they will be automatically converted to JSON.</li>" +
            "</ul>\nIf the columns have different sizes, the last values of the smaller columns will be repeated to fill the whole table. Duplicate keys are overwritten based on the order.")
    @JIPipeParameter(value = "columns", important = true)
    public ParameterCollectionList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ParameterCollectionList columns) {
        this.columns = columns;
    }

    public static class Column extends AbstractJIPipeParameterCollection {
        private String key;
        private JIPipeParameterTypeInfoRef type = new JIPipeParameterTypeInfoRef();
        private DefaultExpressionParameter values = new DefaultExpressionParameter("ARRAY()");
        private boolean valuesAreJson = false;

        public Column() {
        }

        public Column(Column other) {
            this.key = other.key;
            this.type = new JIPipeParameterTypeInfoRef(other.type);
            this.values = new DefaultExpressionParameter(other.values);
            this.valuesAreJson = other.valuesAreJson;
        }

        @JIPipeDocumentation(name = "Key", description = "The parameter key")
        @JIPipeParameter("key")
        @StringParameterSettings(monospace = true)
        public String getKey() {
            return key;
        }

        @JIPipeParameter("key")
        public void setKey(String key) {
            this.key = key;
        }

        @JIPipeDocumentation(name = "Type", description = "The parameter type")
        @JIPipeParameter("type")
        public JIPipeParameterTypeInfoRef getType() {
            return type;
        }

        @JIPipeParameter("type")
        public void setType(JIPipeParameterTypeInfoRef type) {
            this.type = type;
        }

        @JIPipeDocumentation(name = "Values", description = "Generated values")
        @JIPipeParameter("values")
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom filter variables (keys are the parameter keys)")
        @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        public DefaultExpressionParameter getValues() {
            return values;
        }

        @JIPipeParameter("values")
        public void setValues(DefaultExpressionParameter values) {
            this.values = values;
        }

        @JIPipeDocumentation(name = "Values are JSON", description = "Whether values are JSON")
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
