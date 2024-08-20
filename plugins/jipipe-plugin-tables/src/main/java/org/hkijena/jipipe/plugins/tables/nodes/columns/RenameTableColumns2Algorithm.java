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

package org.hkijena.jipipe.plugins.tables.nodes.columns;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.Objects;

/**
 * Algorithm that removes columns
 */
@SetJIPipeDocumentation(name = "Rename table column", description = "Renames columns")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class RenameTableColumns2Algorithm extends JIPipeSimpleIteratingAlgorithm {

    private ParameterCollectionList renamingEntries = ParameterCollectionList.containingCollection(RenamingEntry.class);

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public RenameTableColumns2Algorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameTableColumns2Algorithm(RenameTableColumns2Algorithm other) {
        super(other);
        this.renamingEntries = new ParameterCollectionList(other.renamingEntries);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (RenamingEntry renamingEntry : renamingEntries.mapToCollection(RenamingEntry.class)) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                String oldName = table.getColumnName(col);

                String newName;
                if (renamingEntry.getSourceColumn().test(oldName, variables)) {
                    variables.set("current_name", oldName);
                    newName = renamingEntry.getNewName().evaluateToString(variables);
                } else {
                    newName = oldName;
                }
                if (!Objects.equals(oldName, newName)) {
                    table.renameColumn(oldName, newName);
                }
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
    @StringParameterSettings(monospace = true)
    @PairParameterSettings(singleRow = false, keyLabel = "From", valueLabel = "To")
    @JIPipeParameter("renaming-entries")
    @ParameterCollectionListTemplate(RenamingEntry.class)
    public ParameterCollectionList getRenamingEntries() {
        return renamingEntries;
    }

    @JIPipeParameter("renaming-entries")
    public void setRenamingEntries(ParameterCollectionList renamingEntries) {
        this.renamingEntries = renamingEntries;
    }

    public static class RenamingEntry extends AbstractJIPipeParameterCollection {
        private StringQueryExpression sourceColumn = new StringQueryExpression("\"Old name\"");
        private JIPipeExpressionParameter newName = new JIPipeExpressionParameter("\"New name\"");

        public RenamingEntry() {
        }

        public RenamingEntry(RenamingEntry other) {
            this.sourceColumn = new StringQueryExpression(other.sourceColumn);
            this.newName = new JIPipeExpressionParameter(other.newName);
        }

        @SetJIPipeDocumentation(name = "Column to be renamed", description = "The column to be renamed")
        @JIPipeParameter("source-column")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public StringQueryExpression getSourceColumn() {
            return sourceColumn;
        }

        @JIPipeParameter("source-column")
        public void setSourceColumn(StringQueryExpression sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        @SetJIPipeDocumentation(name = "New name", description = "The new name of the column")
        @JIPipeParameter("new-name")
        @AddJIPipeExpressionParameterVariable(name = "Current name", key = "current_name", description = "The current name")
        @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
        public JIPipeExpressionParameter getNewName() {
            return newName;
        }

        @JIPipeParameter("new-name")
        public void setNewName(JIPipeExpressionParameter newName) {
            this.newName = newName;
        }
    }
}
