package org.hkijena.jipipe.extensions.strings.nodes.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumnNormalization;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Extract JSON values as table", description = "Extracts a value from the input JSON data (via JsonPath) and writes the results into a table. " +
        "Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath")
@JIPipeCitation("JsonPath: https://goessner.net/articles/JsonPath/")
@JIPipeNode(menuPath = "JSON", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JsonData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ExtractJsonDataAsTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);
    private TableColumnNormalization columnNormalization = TableColumnNormalization.ZeroOrEmpty;

    public ExtractJsonDataAsTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public ExtractJsonDataAsTableAlgorithm(ExtractJsonDataAsTableAlgorithm other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
        this.columnNormalization = other.columnNormalization;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        JsonData data = dataBatch.getInputData(getFirstInputSlot(), JsonData.class, progressInfo);
        DocumentContext documentContext = JsonPath.parse(data.getData());

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        List<TableColumn> columns = new ArrayList<>();

        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String path = entry.getJsonPath().evaluateToString(variables);
            String columnName = entry.getColumnName().evaluateToString(variables);
            String columnValue = StringUtils.nullToEmpty(documentContext.read(path));

            if (!StringUtils.isNullOrEmpty(columnValue)) {
                JsonNode jsonNode = JsonUtils.readFromString(columnValue, JsonNode.class);
                if (jsonNode.isArray()) {
                    List<String> values = ImmutableList.copyOf(jsonNode.elements()).stream().map(JsonUtils::toJsonString).collect(Collectors.toList());
                    columns.add(TableColumn.fromList(values, columnName));
                } else {
                    columns.add(TableColumn.fromList(Collections.singleton(columnValue), columnName));
                }
            } else {
                columns.add(new StringArrayTableColumn(new String[0], columnName));
            }
        }

        columns = columnNormalization.normalize(columns);
        ResultsTableData resultsTableData = new ResultsTableData(columns);

        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated columns", description = "The list of generated columns. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @JIPipeDocumentation(name = "Column length normalization", description = "Determines how to fill in missing values if multiple columns are created")
    @JIPipeParameter("column-normalization")
    public TableColumnNormalization getColumnNormalization() {
        return columnNormalization;
    }

    @JIPipeParameter("column-normalization")
    public void setColumnNormalization(TableColumnNormalization columnNormalization) {
        this.columnNormalization = columnNormalization;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private DefaultExpressionParameter jsonPath = new DefaultExpressionParameter("\"$\"");
        private DefaultExpressionParameter columnName = new DefaultExpressionParameter("\"Column name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.jsonPath = new DefaultExpressionParameter(other.jsonPath);
            this.columnName = new DefaultExpressionParameter(other.columnName);
        }

        @JIPipeDocumentation(name = "JSON path", description = "An expression that returns the JsonPath of the JSON entries. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
        @JIPipeParameter(value = "json-path", uiOrder = -100)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        public DefaultExpressionParameter getJsonPath() {
            return jsonPath;
        }

        @JIPipeParameter("json-path")
        public void setJsonPath(DefaultExpressionParameter jsonPath) {
            this.jsonPath = jsonPath;
        }

        @JIPipeDocumentation(name = "Column name", description = "The name of the output column.")
        @JIPipeParameter(value = "column-name", uiOrder = -90)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        public DefaultExpressionParameter getColumnName() {
            return columnName;
        }

        @JIPipeParameter("column-name")
        public void setColumnName(DefaultExpressionParameter columnName) {
            this.columnName = columnName;
        }
    }
}
