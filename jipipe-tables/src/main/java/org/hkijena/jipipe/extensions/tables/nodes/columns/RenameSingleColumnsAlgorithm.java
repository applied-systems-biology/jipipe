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
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

/**
 * Algorithm that removes columns
 */
@JIPipeDocumentation(name = "Rename single columns", description = "Renames columns")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = TableColumn.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class RenameSingleColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpressionAndStringPairParameter.List renamingEntries = new StringQueryExpressionAndStringPairParameter.List();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public RenameSingleColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameSingleColumnsAlgorithm(RenameSingleColumnsAlgorithm other) {
        super(other);
        this.renamingEntries = new StringQueryExpressionAndStringPairParameter.List(other.renamingEntries);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        TableColumn input = iterationStep.getInputData(getFirstInputSlot(), TableColumn.class, progressInfo);
        String name = input.getLabel();
        for (StringQueryExpressionAndStringPairParameter renamingEntry : renamingEntries) {
            if (renamingEntry.getKey().test(name)) {
                name = renamingEntry.getValue();
                break;
            }
        }

        if (input.isNumeric()) {
            iterationStep.addOutputData(getFirstOutputSlot(), new DoubleArrayTableColumn(input.getDataAsDouble(input.getRows()), name), progressInfo);
        } else {
            iterationStep.addOutputData(getFirstOutputSlot(), new StringArrayTableColumn(input.getDataAsString(input.getRows()), name), progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        report.report(new ParameterValidationReportContext(context, this, "Renaming entries", "renaming-entries"), renamingEntries);
    }

    @JIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
    @JIPipeParameter("renaming-entries")
    @PairParameterSettings(singleRow = false, keyLabel = "Old name", valueLabel = "New name")
    public StringQueryExpressionAndStringPairParameter.List getRenamingEntries() {
        return renamingEntries;
    }

    @JIPipeParameter("renaming-entries")
    public void setRenamingEntries(StringQueryExpressionAndStringPairParameter.List renamingEntries) {
        this.renamingEntries = renamingEntries;
    }
}
