package org.hkijena.jipipe.extensions.utils.algorithms;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotationMergeStrategy;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;

import java.util.List;

@JIPipeDocumentation(name = "Select data rows", description = "Allows to select the only a specific set of input data table rows. All other rows are not stored in the output.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
public class SelectDataTableRowsAlgorithm extends JIPipeParameterSlotAlgorithm {

    private IntegerRange limit = new IntegerRange();

    public SelectDataTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SelectDataTableRowsAlgorithm(SelectDataTableRowsAlgorithm other) {
        super(other);
        this.limit = new IntegerRange(other.limit);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        TIntSet allowedRows = new TIntHashSet(limit.getIntegers());
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            if (allowedRows.contains(row)) {
                getFirstOutputSlot().addData(getFirstInputSlot().getVirtualData(row),
                        getFirstInputSlot().getAnnotations(row),
                        JIPipeAnnotationMergeStrategy.Merge,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
            }
        }
    }

    @JIPipeDocumentation(name = "Limit", description = "Determines which indices are passed to the output. The first index is zero.\n" + IntegerRange.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("limit")
    public IntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(IntegerRange limit) {
        this.limit = limit;
    }
}
