package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;

import java.util.ArrayList;
import java.util.HashSet;

@JIPipeDocumentation(name = "Data batch slicer", description = "Merges the incoming data into merging data batches. Only outputs " +
        "the items according to the index range. You can use this to remove duplicates. Annotations are not modified (merged annotations are not copied into " +
        "the output).")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
public class DataBatchSlicer extends JIPipeMergingAlgorithm {

    private IntegerRange sliceRange = new IntegerRange("0");

    public DataBatchSlicer(JIPipeNodeInfo info) {
        super(info);
    }

    public DataBatchSlicer(DataBatchSlicer other) {
        super(other);
        this.sliceRange = new IntegerRange(other.sliceRange);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        HashSet<Integer> indices = new HashSet<>(sliceRange.getIntegers());
        ArrayList<Integer> rows = new ArrayList<>(dataBatch.getInputRows(getFirstInputSlot()));
        for (int i = 0; i < rows.size(); i++) {
            if(indices.contains(i)) {
                getFirstOutputSlot().addData(getFirstInputSlot().getVirtualData(i),
                        getFirstInputSlot().getAnnotations(i),
                        JIPipeAnnotationMergeStrategy.OverwriteExisting);
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
