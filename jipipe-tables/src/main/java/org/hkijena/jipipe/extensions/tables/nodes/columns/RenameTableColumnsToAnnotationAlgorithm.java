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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringQueryPairParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * Algorithm that removes columns
 */
@SetJIPipeDocumentation(name = "Rename column to annotation values", description = "Sets the name of specified columns to the value of the specified annotations.")
@DefineJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        for (StringQueryExpressionAndStringQueryPairParameter renamingEntry : renamingEntries) {
            String oldName = renamingEntry.getKey().queryFirst(input.getColumnNames(), new JIPipeExpressionVariablesMap());
            String newName = renamingEntry.getValue().queryFirst(iterationStep.getMergedTextAnnotations().keySet(), new JIPipeExpressionVariablesMap());
            if (oldName == null) {
                if (ignoreMissingColumns)
                    continue;
                throw new JIPipeValidationRuntimeException(new NullPointerException("Could not find column matching '" + renamingEntry.getKey() + "'"),
                        "Could not find column matching '" + renamingEntry.getKey() + "'",
                        "You tried to rename a column '" + renamingEntry.getKey() + "', but it was not found.",
                        "Please check if the table '" + input + "' contains the column.");
            }
            if (newName == null) {
                if (ignoreMissingAnnotations)
                    continue;
                throw new JIPipeValidationRuntimeException(new NullPointerException("Could not find annotation matching '" + renamingEntry.getValue() + "'"),
                        "Could not find annotation matching '" + renamingEntry.getValue() + "'",
                        "You tried to rename a column '" + renamingEntry.getKey() + "' to the value of annotation '" + renamingEntry.getValue()
                                + "', but the annotation was not found.",
                        "Please check if there is a matching annotation.");
            }
            newName = iterationStep.getMergedTextAnnotation(newName).getValue();
            input.renameColumn(oldName, newName);
        }
        iterationStep.addOutputData(getFirstOutputSlot(), input, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Renaming entries", "renaming-entries"), renamingEntries);
    }

    @SetJIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
    @JIPipeParameter("renaming-entries")
    @PairParameterSettings(singleRow = false, keyLabel = "Column name", valueLabel = "Annotation name")
    public StringQueryExpressionAndStringQueryPairParameter.List getRenamingEntries() {
        return renamingEntries;
    }

    @JIPipeParameter("renaming-entries")
    public void setRenamingEntries(StringQueryExpressionAndStringQueryPairParameter.List renamingEntries) {
        this.renamingEntries = renamingEntries;
    }

    @SetJIPipeDocumentation(name = "Ignore missing columns", description = "If enabled, silently skip columns that could not be found")
    @JIPipeParameter("ignore-missing-columns")
    public boolean isIgnoreMissingColumns() {
        return ignoreMissingColumns;
    }

    @JIPipeParameter("ignore-missing-columns")
    public void setIgnoreMissingColumns(boolean ignoreMissingColumns) {
        this.ignoreMissingColumns = ignoreMissingColumns;
    }

    @SetJIPipeDocumentation(name = "Ignore missing annotations", description = "If enabled, silently skip renaming operations where the annotation could not be found")
    @JIPipeParameter("ignore-missing-annotations")
    public boolean isIgnoreMissingAnnotations() {
        return ignoreMissingAnnotations;
    }

    @JIPipeParameter("ignore-missing-annotations")
    public void setIgnoreMissingAnnotations(boolean ignoreMissingAnnotations) {
        this.ignoreMissingAnnotations = ignoreMissingAnnotations;
    }
}
