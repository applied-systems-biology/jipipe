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

package org.hkijena.jipipe.extensions.strings.nodes.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
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

@SetJIPipeDocumentation(name = "Extract JSON values as table", description = "Extracts a value from the input JSON data (via JsonPath) and writes the results into a table. " +
        "Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath")
@AddJIPipeCitation("JsonPath: https://goessner.net/articles/JsonPath/")
@ConfigureJIPipeNode(menuPath = "JSON", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JsonData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JsonData data = iterationStep.getInputData(getFirstInputSlot(), JsonData.class, progressInfo);
        DocumentContext documentContext = JsonPath.parse(data.getData());

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

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

        iterationStep.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generated columns", description = "The list of generated columns. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    @SetJIPipeDocumentation(name = "Column length normalization", description = "Determines how to fill in missing values if multiple columns are created")
    @JIPipeParameter("column-normalization")
    public TableColumnNormalization getColumnNormalization() {
        return columnNormalization;
    }

    @JIPipeParameter("column-normalization")
    public void setColumnNormalization(TableColumnNormalization columnNormalization) {
        this.columnNormalization = columnNormalization;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private JIPipeExpressionParameter jsonPath = new JIPipeExpressionParameter("\"$\"");
        private JIPipeExpressionParameter columnName = new JIPipeExpressionParameter("\"Column name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.jsonPath = new JIPipeExpressionParameter(other.jsonPath);
            this.columnName = new JIPipeExpressionParameter(other.columnName);
        }

        @SetJIPipeDocumentation(name = "JSON path", description = "An expression that returns the JsonPath of the JSON entries. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
        @JIPipeParameter(value = "json-path", uiOrder = -100)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getJsonPath() {
            return jsonPath;
        }

        @JIPipeParameter("json-path")
        public void setJsonPath(JIPipeExpressionParameter jsonPath) {
            this.jsonPath = jsonPath;
        }

        @SetJIPipeDocumentation(name = "Column name", description = "The name of the output column.")
        @JIPipeParameter(value = "column-name", uiOrder = -90)
        @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getColumnName() {
            return columnName;
        }

        @JIPipeParameter("column-name")
        public void setColumnName(JIPipeExpressionParameter columnName) {
            this.columnName = columnName;
        }
    }
}
