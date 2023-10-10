/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.tables.nodes.columns;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionList;
import org.hkijena.jipipe.extensions.parameters.library.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Objects;

/**
 * Algorithm that removes columns
 */
@JIPipeDocumentation(name = "Rename table column", description = "Renames columns")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
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
        dataBatch.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @JIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
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
        private DefaultExpressionParameter newName = new DefaultExpressionParameter("\"New name\"");

        public RenamingEntry() {
        }

        public RenamingEntry(RenamingEntry other) {
            this.sourceColumn = new StringQueryExpression(other.sourceColumn);
            this.newName = new DefaultExpressionParameter(other.newName);
        }

        @JIPipeDocumentation(name = "Column to be renamed", description = "The column to be renamed")
        @JIPipeParameter("source-column")
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        public StringQueryExpression getSourceColumn() {
            return sourceColumn;
        }

        @JIPipeParameter("source-column")
        public void setSourceColumn(StringQueryExpression sourceColumn) {
            this.sourceColumn = sourceColumn;
        }

        @JIPipeDocumentation(name = "New name", description = "The new name of the column")
        @JIPipeParameter("new-name")
        @ExpressionParameterSettingsVariable(name = "Current name", key = "current_name", description = "The current name")
        @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
        public DefaultExpressionParameter getNewName() {
            return newName;
        }

        @JIPipeParameter("new-name")
        public void setNewName(DefaultExpressionParameter newName) {
            this.newName = newName;
        }
    }
}
