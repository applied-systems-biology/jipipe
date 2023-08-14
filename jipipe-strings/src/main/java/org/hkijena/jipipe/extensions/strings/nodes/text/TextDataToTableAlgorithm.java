package org.hkijena.jipipe.extensions.strings.nodes.text;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumnNormalization;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Text to table", description = "Extracts a values from the input text data (via an expression) and writes the results into a table.")
@JIPipeNode(menuPath = "Text", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = StringData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        StringData data = dataBatch.getInputData(getFirstInputSlot(), StringData.class, progressInfo);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

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

        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated columns", description = "The list of generated columns.")
    @JIPipeParameter("entries")
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
        private DefaultExpressionParameter preprocessor = new DefaultExpressionParameter("text");
        private DefaultExpressionParameter columnName = new DefaultExpressionParameter("\"Column name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.preprocessor = new DefaultExpressionParameter(other.preprocessor);
            this.columnName = new DefaultExpressionParameter(other.columnName);
        }

        @JIPipeDocumentation(name = "Preprocessor", description = "An expression that allows to preprocess the text. You can return an ARRAY to create multiple rows.")
        @JIPipeParameter(value = "preprocessor", uiOrder = -100)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Text", key = "text", description = "The input text")
        public DefaultExpressionParameter getPreprocessor() {
            return preprocessor;
        }

        @JIPipeParameter("preprocessor")
        public void setPreprocessor(DefaultExpressionParameter preprocessor) {
            this.preprocessor = preprocessor;
        }

        @JIPipeDocumentation(name = "Column name", description = "The name of the output column.")
        @JIPipeParameter(value = "column-name", uiOrder = -90)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        @ExpressionParameterSettingsVariable(name = "Text", key = "text", description = "The input text")
        public DefaultExpressionParameter getColumnName() {
            return columnName;
        }

        @JIPipeParameter("column-name")
        public void setColumnName(DefaultExpressionParameter columnName) {
            this.columnName = columnName;
        }
    }
}