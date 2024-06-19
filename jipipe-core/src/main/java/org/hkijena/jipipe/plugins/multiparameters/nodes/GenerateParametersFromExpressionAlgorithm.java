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

package org.hkijena.jipipe.plugins.multiparameters.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeParameterTypeInfoRef;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.*;

@SetJIPipeDocumentation(name = "Generate parameters from expression", description = "Generates a table of parameters from expressions defined in the 'Generated parameter columns' parameter.")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Annotations", description = "Optional data that act as source for annotations.", create = true, optional = true)
@AddJIPipeOutputSlot(value = ParametersData.class, slotName = "Parameters", description = "Generated parameters", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class GenerateParametersFromExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList columns;

    public GenerateParametersFromExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        columns = ParameterCollectionList.containingCollection(Column.class);
        columns.addNewInstance();
    }

    public GenerateParametersFromExpressionAlgorithm(GenerateParametersFromExpressionAlgorithm other) {
        super(other);
        columns = new ParameterCollectionList(other.columns);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        for (Column column : columns.mapToCollection(Column.class)) {
            if (StringUtils.isNullOrEmpty(column.key)) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        reportContext,
                        "Column key cannot be empty!",
                        "You cannot have empty parameter keys!",
                        "Provide an appropriate parameter key."));
            }
            if (column.type.getInfo() == null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        reportContext,
                        "Column type cannot be empty!",
                        "You cannot have empty parameter type!",
                        "Provide an appropriate parameter type."));
            }
        }
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        // Generate variables
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        // Generate columns
        Map<String, List<Object>> valueMap = new HashMap<>();
        int nRow = 0;
        for (Column column : columns.mapToCollection(Column.class)) {
            Object evaluationResult = column.values.evaluate(variables);
            List<Object> values = new ArrayList<>();
            if (evaluationResult instanceof Collection) {
                for (Object o : ((Collection<?>) evaluationResult)) {
                    values.add(parseColumnValue(o, column));
                }
            } else {
                values.add(parseColumnValue(evaluationResult, column));
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

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Generated parameter columns", description = "Each item within this list defines a column in the generated parameter table. You must set the following properties: " +
            "<ul>" +
            "<li>Key: the internal key of the parameter that is referred to. You can look it up by clicking the (?) icon next to a parameter and reading the technical info in the documentation that will be shown.</li>" +
            "<li>Type: the data type of the parameter. Please ensure to select the correct type.</li>" +
            "<li>Values: an expression that should generate a string or an array/list of values. The expected value(s) are dependent on the 'Values are JSON' parameter.</li>" +
            "<li>Values are JSON: determines whether the values from the 'Values' parameter are in JSON format or already of the expected object type. If 'Values are JSON' is enabled and non-string values are returned (e.g., a map), they will be automatically converted to JSON.</li>" +
            "</ul>\nIf the columns have different sizes, the last values of the smaller columns will be repeated to fill the whole table. Duplicate keys are overwritten based on the order.")
    @JIPipeParameter(value = "columns", important = true)
    @ParameterCollectionListTemplate(Column.class)
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
        private JIPipeExpressionParameter values = new JIPipeExpressionParameter("ARRAY()");
        private boolean valuesAreJson = false;

        public Column() {
        }

        public Column(Column other) {
            this.key = other.key;
            this.type = new JIPipeParameterTypeInfoRef(other.type);
            this.values = new JIPipeExpressionParameter(other.values);
            this.valuesAreJson = other.valuesAreJson;
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

        @SetJIPipeDocumentation(name = "Values", description = "Generated values")
        @JIPipeParameter("values")
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
        @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
        public JIPipeExpressionParameter getValues() {
            return values;
        }

        @JIPipeParameter("values")
        public void setValues(JIPipeExpressionParameter values) {
            this.values = values;
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
