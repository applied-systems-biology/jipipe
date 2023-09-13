package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.ome;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.OMEAccessorParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

@JIPipeDocumentation(name = "OME metadata as table", description = "Extracts OME metadata as table. The columns and extracted metadata can be freely chosen.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = OMEImageData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class OMEMetadataToTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private ParameterCollectionList entries = ParameterCollectionList.containingCollection(Entry.class);

    public OMEMetadataToTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
        entries.addNewInstance();
    }

    public OMEMetadataToTableAlgorithm(OMEMetadataToTableAlgorithm other) {
        super(other);
        this.entries = new ParameterCollectionList(other.entries);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        OMEImageData inputData = dataBatch.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        ResultsTableData outputData = new ResultsTableData();
        outputData.addRow();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String columnName = entry.getColumnName().evaluateToString(variables);
            Object columnValue = StringUtils.tryParseDoubleOrReturnString(StringUtils.nullToEmpty(entry.getAccessor().evaluateToString(inputData.getMetadata())));
            outputData.setValueAt(columnValue, 0, columnName);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Generated annotations", description = "The list of generated annotations.")
    @JIPipeParameter("entries")
    @ParameterCollectionListTemplate(Entry.class)
    public ParameterCollectionList getEntries() {
        return entries;
    }

    @JIPipeParameter("entries")
    public void setEntries(ParameterCollectionList entries) {
        this.entries = entries;
    }

    public static class Entry extends AbstractJIPipeParameterCollection {
        private OMEAccessorParameter accessor = new OMEAccessorParameter();
        private DefaultExpressionParameter columnName = new DefaultExpressionParameter("\"Column name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.accessor = new OMEAccessorParameter(other.accessor);
            this.columnName = new DefaultExpressionParameter(other.columnName);
        }

        @JIPipeDocumentation(name = "OME metadata", description = "The metadata to query from OME")
        @JIPipeParameter(value = "accessor", uiOrder = -100)
        public OMEAccessorParameter getAccessor() {
            return accessor;
        }

        @JIPipeParameter("accessor")
        public void setAccessor(OMEAccessorParameter accessor) {
            this.accessor = accessor;
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
