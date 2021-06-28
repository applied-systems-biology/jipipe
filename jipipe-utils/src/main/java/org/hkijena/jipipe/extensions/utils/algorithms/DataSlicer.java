package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@JIPipeDocumentation(name = "Limit data", description = "Slices the incoming data table into a smaller table with a limited row set according to the selected row.")
@JIPipeInputSlot(value = JIPipeData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", inheritedSlot = "Input", autoCreate = true)
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
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
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {
        HashSet<Integer> indices = new HashSet<>(sliceRange.getIntegers());
        for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
            if (indices.contains(i)) {
                getFirstOutputSlot().addData(getFirstInputSlot().getVirtualData(i),
                        getFirstInputSlot().getAnnotations(i),
                        JIPipeAnnotationMergeStrategy.OverwriteExisting);
            }
        }
    }

    @JIPipeDocumentation(name = "Slice range", description = "Only the data rows within this range are copied into the output. The first index is zero. " +
            "Invalid indices are ignored.\n\n" + IntegerRange.DOCUMENTATION_DESCRIPTION)
    @JIPipeParameter("slice-range")
    public IntegerRange getSliceRange() {
        return sliceRange;
    }

    @JIPipeParameter("slice-range")
    public void setSliceRange(IntegerRange sliceRange) {
        this.sliceRange = sliceRange;
    }
}