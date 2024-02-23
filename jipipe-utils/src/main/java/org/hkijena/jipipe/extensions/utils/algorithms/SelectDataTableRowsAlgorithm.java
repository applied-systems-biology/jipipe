package org.hkijena.jipipe.extensions.utils.algorithms;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.List;

@SetJIPipeDocumentation(name = "Select data rows", description = "Allows to select the only a specific set of input data table rows. All other rows are not stored in the output.")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class SelectDataTableRowsAlgorithm extends JIPipeParameterSlotAlgorithm {

    private IntegerRange limit = new IntegerRange("0-10");

    public SelectDataTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SelectDataTableRowsAlgorithm(SelectDataTableRowsAlgorithm other) {
        super(other);
        this.limit = new IntegerRange(other.limit);
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        TIntSet allowedRows = new TIntHashSet(limit.getIntegers(0, getFirstInputSlot().getRowCount(), new JIPipeExpressionVariablesMap()));
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            if (allowedRows.contains(row)) {
                getFirstOutputSlot().addData(getFirstInputSlot().getDataItemStore(row),
                        getFirstInputSlot().getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.Merge,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataContext(row).branch(this),
                        progressInfo);
            }
        }
    }

    @SetJIPipeDocumentation(name = "Limit", description = "Determines which indices are passed to the output. The first index is zero.")
    @JIPipeParameter("limit")
    public IntegerRange getLimit() {
        return limit;
    }

    @JIPipeParameter("limit")
    public void setLimit(IntegerRange limit) {
        this.limit = limit;
    }
}
