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

package org.hkijena.jipipe.plugins.strings.nodes.text;

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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnNormalization;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Text to table", description = "Extracts a values from the input text data (via an expression) and writes the results into a table.")
@ConfigureJIPipeNode(menuPath = "Text", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = StringData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class TextDataToTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);
    private TableColumnNormalization columnNormalization = TableColumnNormalization.ZeroOrEmpty;

    public TextDataToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public TextDataToTableAlgorithm(TextDataToTableAlgorithm other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
        this.columnNormalization = other.columnNormalization;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        StringData data = iterationStep.getInputData(getFirstInputSlot(), StringData.class, progressInfo);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        List<TableColumn> columns = new ArrayList<>();

        for (Entry entry : entries.mapToCollection(Entry.class)) {

            variables.put("text", data.getData());

            String columnName = entry.getColumnName().evaluateToString(variables);
            Object columnValue = entry.preprocessor.evaluate(variables);

            if (columnValue instanceof Collection) {
                List<String> values = ((Collection<?>) columnValue).stream().map(StringUtils::nullToEmpty).collect(Collectors.toList());
                columns.add(TableColumn.fromList(values, columnName));
            } else {
                columns.add(new StringArrayTableColumn(new String[]{StringUtils.nullToEmpty(columnValue)}, columnName));
            }
        }

        columns = columnNormalization.normalize(columns);
        ResultsTableData resultsTableData = new ResultsTableData(columns);

        iterationStep.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generated columns", description = "The list of generated columns.")
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
        private JIPipeExpressionParameter preprocessor = new JIPipeExpressionParameter("text");
        private JIPipeExpressionParameter columnName = new JIPipeExpressionParameter("\"Column name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.preprocessor = new JIPipeExpressionParameter(other.preprocessor);
            this.columnName = new JIPipeExpressionParameter(other.columnName);
        }

        @SetJIPipeDocumentation(name = "Preprocessor", description = "An expression that allows to preprocess the text. You can return an ARRAY to create multiple rows.")
        @JIPipeParameter(value = "preprocessor", uiOrder = -100)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Text", key = "text", description = "The input text")
        public JIPipeExpressionParameter getPreprocessor() {
            return preprocessor;
        }

        @JIPipeParameter("preprocessor")
        public void setPreprocessor(JIPipeExpressionParameter preprocessor) {
            this.preprocessor = preprocessor;
        }

        @SetJIPipeDocumentation(name = "Column name", description = "The name of the output column.")
        @JIPipeParameter(value = "column-name", uiOrder = -90)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        @AddJIPipeExpressionParameterVariable(name = "Text", key = "text", description = "The input text")
        public JIPipeExpressionParameter getColumnName() {
            return columnName;
        }

        @JIPipeParameter("column-name")
        public void setColumnName(JIPipeExpressionParameter columnName) {
            this.columnName = columnName;
        }
    }
}
