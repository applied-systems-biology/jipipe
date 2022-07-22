package org.hkijena.jipipe.extensions.utils.algorithms;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.List;

@JIPipeDocumentation(name = "Select data rows", description = "Allows to select the only a specific set of input data table rows. All other rows are not stored in the output.")
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter")
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
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        TIntSet allowedRows = new TIntHashSet(limit.getIntegers(0, getFirstInputSlot().getRowCount(), new ExpressionVariables()));
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            if (allowedRows.contains(row)) {
                getFirstOutputSlot().addData(getFirstInputSlot().getVirtualData(row),
                        getFirstInputSlot().getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.Merge,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting);
            }
        }
    }

    @JIPipeDocumentation(name = "Limit", description = "Determines which indices are passed to the output. The first index is zero.")
    @JIPipeParameter("limit")
    public IntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(IntegerRange limit) {
        this.limit = limit;
    }
}
