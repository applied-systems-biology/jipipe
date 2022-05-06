package org.hkijena.jipipe.extensions.utils.algorithms;

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
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.HashSet;
import java.util.List;

@JIPipeDocumentation(name = "Limit data", description = "Slices the incoming data table into a smaller table with a limited row set according to the selected row.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter")
public class DataSlicer extends JIPipeParameterSlotAlgorithm {

    private IntegerRange sliceRange = new IntegerRange("0-999");

    public DataSlicer(JIPipeNodeInfo info) {
        super(info);
    }

    public DataSlicer(DataSlicer other) {
        super(other);
        this.sliceRange = new IntegerRange(other.sliceRange);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        HashSet<Integer> indices = new HashSet<>(sliceRange.getIntegers(0, getFirstInputSlot().getRowCount()));
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            if (indices.contains(row)) {
                getFirstOutputSlot().addData(getFirstInputSlot().getVirtualData(row),
                        getFirstInputSlot().getTextAnnotations(row),
                        JIPipeTextAnnotationMergeMode.OverwriteExisting,
                        getFirstInputSlot().getDataAnnotations(row),
                        JIPipeDataAnnotationMergeMode.OverwriteExisting);
            }
        }
    }

    @JIPipeDocumentation(name = "Slice range", description = "Only the data rows within this range are copied into the output. The first index is zero. " +
            "Invalid indices are ignored.")
    @JIPipeParameter("slice-range")
    public IntegerRange getSliceRange() {
        return sliceRange;
    }

    @JIPipeParameter("slice-range")
    public void setSliceRange(IntegerRange sliceRange) {
        this.sliceRange = sliceRange;
    }
}
