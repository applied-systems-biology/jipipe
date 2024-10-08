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
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumnData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

/**
 * Algorithm that removes columns
 */
@SetJIPipeDocumentation(name = "Rename single columns", description = "Renames columns")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = TableColumnData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = TableColumnData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        TableColumnData input = iterationStep.getInputData(getFirstInputSlot(), TableColumnData.class, progressInfo);
        String name = input.getLabel();
        for (StringQueryExpressionAndStringPairParameter renamingEntry : renamingEntries) {
            if (renamingEntry.getKey().test(name)) {
                name = renamingEntry.getValue();
                break;
            }
        }

        if (input.isNumeric()) {
            iterationStep.addOutputData(getFirstOutputSlot(), new DoubleArrayTableColumnData(input.getDataAsDouble(input.getRows()), name), progressInfo);
        } else {
            iterationStep.addOutputData(getFirstOutputSlot(), new StringArrayTableColumnData(input.getDataAsString(input.getRows()), name), progressInfo);
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Renaming entries", "renaming-entries"), renamingEntries);
    }

    @SetJIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
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
