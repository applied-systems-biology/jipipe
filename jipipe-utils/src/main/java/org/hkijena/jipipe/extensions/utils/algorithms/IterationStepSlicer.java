package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.ArrayList;
import java.util.HashSet;

@JIPipeDocumentation(name = "Data batch slicer", description = "Merges the incoming data into merging data batches. Only outputs " +
        "the items according to the index range. You can use this to remove duplicates. Annotations are not modified (merged annotations are not copied into " +
        "the output).")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter")
public class IterationStepSlicer extends JIPipeMergingAlgorithm {

    private IntegerRange sliceRange = new IntegerRange("0");

    public IterationStepSlicer(JIPipeNodeInfo info) {
        super(info);
    }

    public IterationStepSlicer(IterationStepSlicer other) {
        super(other);
        this.sliceRange = new IntegerRange(other.sliceRange);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ArrayList<Integer> rows = new ArrayList<>(iterationStep.getInputRows(getFirstInputSlot()));
        HashSet<Integer> indices = new HashSet<>(sliceRange.getIntegers(0, rows.size(), new JIPipeExpressionVariablesMap()));
        for (int i = 0; i < rows.size(); i++) {
            if (indices.contains(i)) {
                int row = rows.get(i);
                getFirstOutputSlot().addData(getFirstInputSlot().getDataItemStore(row),
                        getFirstInputSlot().getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataContext(row).branch(this),
                        progressInfo);
            }
        }
    }

    @JIPipeDocumentation(name = "Slice range", description = "Only the items within this range are copied into the output. The first index is zero. " +
            "If a data batch does not contain the requested index, no data is copied.")
    @JIPipeParameter("slice-range")
    public IntegerRange getSliceRange() {
        return sliceRange;
    }

    @JIPipeParameter("slice-range")
    public void setSliceRange(IntegerRange sliceRange) {
        this.sliceRange = sliceRange;
    }
}
