package org.hkijena.jipipe.extensions.strings.nodes.xml;


import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.strings.XMLData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumnNormalization;
import org.hkijena.jipipe.utils.xml.XmlUtils;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Extract XML values as table", description = "Extracts a value from the input text data (via XPath) and writes the results into a table. " +
        "Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn about XPath.")
@JIPipeCitation("XPath: https://www.w3schools.com/xml/xpath_intro.asp")
@JIPipeNode(menuPath = "XML", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = XMLData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ExtractXPathDataAsTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);
    private TableColumnNormalization columnNormalization = TableColumnNormalization.ZeroOrEmpty;
    private StringAndStringPairParameter.List namespaceMap = new StringAndStringPairParameter.List();

    public ExtractXPathDataAsTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public ExtractXPathDataAsTableAlgorithm(ExtractXPathDataAsTableAlgorithm other) {
        super(other);
        this.namespaceMap = new StringAndStringPairParameter.List(other.namespaceMap);
        this.entries = new ParameterCollectionList(other.entries);
        this.columnNormalization = other.columnNormalization;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        XMLData data = dataBatch.getInputData(getFirstInputSlot(), XMLData.class, progressInfo);
        Document document = XmlUtils.readFromString(data.getData());

        Map<String, String> namespaces = new HashMap<>();
        for (StringAndStringPairParameter parameter : namespaceMap) {
            namespaces.put(parameter.getKey(), parameter.getValue());
        }

        List<TableColumn> columns = new ArrayList<>();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String path = entry.getxPath().evaluateToString(variables);
            String columnName = entry.getColumnName().evaluateToString(variables);

            List<String> annotationValue = XmlUtils.extractStringListFromXPath(document, path, namespaces);
            columns.add(new StringArrayTableColumn(annotationValue.toArray(new String[0]), columnName));
        }

        columns = columnNormalization.normalize(columns);
        ResultsTableData resultsTableData = new ResultsTableData(columns);

        dataBatch.addOutputData(getFirstOutputSlot(), resultsTableData, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated columns", description = "The list of generated columns. Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn more about XPath.")
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

    @JIPipeDocumentation(name = "Namespace map", description = "Allows to map namespaces to shortcuts for more convenient access")
    @JIPipeParameter("namespace-map")
    @PairParameterSettings(keyLabel = "Shortcut", valueLabel = "Namespace")
    public StringAndStringPairParameter.List getNamespaceMap() {
        return namespaceMap;
    }

    @JIPipeParameter("namespace-map")
    public void setNamespaceMap(StringAndStringPairParameter.List namespaceMap) {
        this.namespaceMap = namespaceMap;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private DefaultExpressionParameter xPath = new DefaultExpressionParameter("\"/\"");
        private DefaultExpressionParameter columnName = new DefaultExpressionParameter("\"Column name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.xPath = new DefaultExpressionParameter(other.xPath);
            this.columnName = new DefaultExpressionParameter(other.columnName);
        }

        @JIPipeDocumentation(name = "XPath", description = "An expression that returns the XPath of the XML entries. Please visit https://www.w3schools.com/xml/xpath_intro.asp to learn more about XPath.")
        @JIPipeParameter(value = "xpath", uiOrder = -100)
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        public DefaultExpressionParameter getxPath() {
            return xPath;
        }

        @JIPipeParameter("xpath")
        public void setxPath(DefaultExpressionParameter xPath) {
            this.xPath = xPath;
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
