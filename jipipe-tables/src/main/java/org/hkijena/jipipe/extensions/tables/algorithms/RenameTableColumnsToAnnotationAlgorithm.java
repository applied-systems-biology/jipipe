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
 */

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyNullPointerException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringQueryPairParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * Algorithm that removes columns
 */
@JIPipeDocumentation(name = "Rename column to annotation values", description = "Sets the name of specified columns to the value of the specified annotations.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class RenameTableColumnsToAnnotationAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpressionAndStringQueryPairParameter.List renamingEntries = new StringQueryExpressionAndStringQueryPairParameter.List();
    private boolean ignoreMissingColumns = false;
    private boolean ignoreMissingAnnotations = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public RenameTableColumnsToAnnotationAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameTableColumnsToAnnotationAlgorithm(RenameTableColumnsToAnnotationAlgorithm other) {
        super(other);
        this.renamingEntries = new StringQueryExpressionAndStringQueryPairParameter.List(other.renamingEntries);
        this.ignoreMissingColumns = other.ignoreMissingColumns;
        this.ignoreMissingAnnotations = other.ignoreMissingAnnotations;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = (ResultsTableData) dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate();
        for (StringQueryExpressionAndStringQueryPairParameter renamingEntry : renamingEntries) {
            String oldName = renamingEntry.getKey().queryFirst(input.getColumnNames(), new ExpressionParameters());
            String newName = renamingEntry.getValue().queryFirst(dataBatch.getAnnotations().keySet(), new ExpressionParameters());
            if (oldName == null) {
                if (ignoreMissingColumns)
                    continue;
                throw new UserFriendlyNullPointerException("Could not find column matching '" + renamingEntry.getKey() + "'",
                        "Could not find column!",
                        "Algorithm " + getName(),
                        "You tried to rename a column '" + renamingEntry.getKey() + "', but it was not found.",
                        "Please check if the table '" + input + "' contains the column.");
            }
            if (newName == null) {
                if (ignoreMissingAnnotations)
                    continue;
                throw new UserFriendlyNullPointerException("Could not find annotation matching '" + renamingEntry.getValue() + "'",
                        "Could not find annotation!",
                        "Algorithm " + getName(),
                        "You tried to rename a column '" + renamingEntry.getKey() + "' to the value of annotation '" + renamingEntry.getValue()
                                + "', but the annotation was not found.",
                        "Please check if there is a matching annotation.");
            }
            newName = dataBatch.getAnnotationOfType(newName).getValue();
            input.renameColumn(oldName, newName);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), input, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Renaming entries").report(renamingEntries);
    }

    @JIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
    @JIPipeParameter("renaming-entries")
    @PairParameterSettings(singleRow = false, keyLabel = "Column name", valueLabel = "Annotation name")
    public StringQueryExpressionAndStringQueryPairParameter.List getRenamingEntries() {
        return renamingEntries;
    }

    @JIPipeParameter("renaming-entries")
    public void setRenamingEntries(StringQueryExpressionAndStringQueryPairParameter.List renamingEntries) {
        this.renamingEntries = renamingEntries;
    }

    @JIPipeDocumentation(name = "Ignore missing columns", description = "If enabled, silently skip columns that could not be found")
    @JIPipeParameter("ignore-missing-columns")
    public boolean isIgnoreMissingColumns() {
        return ignoreMissingColumns;
    }

    @JIPipeParameter("ignore-missing-columns")
    public void setIgnoreMissingColumns(boolean ignoreMissingColumns) {
        this.ignoreMissingColumns = ignoreMissingColumns;
    }

    @JIPipeDocumentation(name = "Ignore missing annotations", description = "If enabled, silently skip renaming operations where the annotation could not be found")
    @JIPipeParameter("ignore-missing-annotations")
    public boolean isIgnoreMissingAnnotations() {
        return ignoreMissingAnnotations;
    }

    @JIPipeParameter("ignore-missing-annotations")
    public void setIgnoreMissingAnnotations(boolean ignoreMissingAnnotations) {
        this.ignoreMissingAnnotations = ignoreMissingAnnotations;
    }
}
