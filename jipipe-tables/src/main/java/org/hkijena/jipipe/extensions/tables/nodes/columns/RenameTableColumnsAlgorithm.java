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

package org.hkijena.jipipe.extensions.tables.nodes.columns;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
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
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Objects;

/**
 * Algorithm that removes columns
 */
@SetJIPipeDocumentation(name = "Rename table column", description = "Renames columns")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
@Deprecated
@LabelAsJIPipeHidden
public class RenameTableColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpressionAndStringPairParameter.List renamingEntries = new StringQueryExpressionAndStringPairParameter.List();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public RenameTableColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RenameTableColumnsAlgorithm(RenameTableColumnsAlgorithm other) {
        super(other);
        this.renamingEntries = new StringQueryExpressionAndStringPairParameter.List(other.renamingEntries);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        for (StringQueryExpressionAndStringPairParameter renamingEntry : renamingEntries) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                String oldName = table.getColumnName(col);
                String newName = renamingEntry.getKey().test(oldName) ? renamingEntry.getValue() : oldName;
                if (!Objects.equals(oldName, newName)) {
                    table.renameColumn(oldName, newName);
                }
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Renaming entries", "renaming-entries"), renamingEntries);
    }

    @SetJIPipeDocumentation(name = "Renaming entries", description = "You can rename one or multiple columns.")
    @StringParameterSettings(monospace = true)
    @PairParameterSettings(singleRow = false, keyLabel = "From", valueLabel = "To")
    @JIPipeParameter("renaming-entries")
    public StringQueryExpressionAndStringPairParameter.List getRenamingEntries() {
        return renamingEntries;
    }

    @JIPipeParameter("renaming-entries")
    public void setRenamingEntries(StringQueryExpressionAndStringPairParameter.List renamingEntries) {
        this.renamingEntries = renamingEntries;
    }
}
