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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.ome;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.OMEAccessorParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

@SetJIPipeDocumentation(name = "OME metadata as table", description = "Extracts OME metadata as table. The columns and extracted metadata can be freely chosen.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = OMEImageData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        OMEImageData inputData = iterationStep.getInputData(getFirstInputSlot(), OMEImageData.class, progressInfo);
        ResultsTableData outputData = new ResultsTableData();
        outputData.addRow();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (Entry entry : entries.mapToCollection(Entry.class)) {
            String columnName = entry.getColumnName().evaluateToString(variables);
            Object columnValue = StringUtils.tryParseDoubleOrReturnString(StringUtils.nullToEmpty(entry.getAccessor().evaluateToString(inputData.getMetadata())));
            outputData.setValueAt(columnValue, 0, columnName);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Generated annotations", description = "The list of generated annotations.")
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
        private JIPipeExpressionParameter columnName = new JIPipeExpressionParameter("\"Column name\"");

        public Entry() {
        }

        public Entry(Entry other) {
            this.accessor = new OMEAccessorParameter(other.accessor);
            this.columnName = new JIPipeExpressionParameter(other.columnName);
        }

        @SetJIPipeDocumentation(name = "OME metadata", description = "The metadata to query from OME")
        @JIPipeParameter(value = "accessor", uiOrder = -100)
        public OMEAccessorParameter getAccessor() {
            return accessor;
        }

        @JIPipeParameter("accessor")
        public void setAccessor(OMEAccessorParameter accessor) {
            this.accessor = accessor;
        }

        @SetJIPipeDocumentation(name = "Column name", description = "The name of the output column.")
        @JIPipeParameter(value = "column-name", uiOrder = -90)
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getColumnName() {
            return columnName;
        }

        @JIPipeParameter("column-name")
        public void setColumnName(JIPipeExpressionParameter columnName) {
            this.columnName = columnName;
        }
    }
}
