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

package org.hkijena.jipipe.extensions.utils.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
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

@SetJIPipeDocumentation(name = "Data batch slicer", description = "Merges the incoming data into merging data batches. Only outputs " +
        "the items according to the index range. You can use this to remove duplicates. Annotations are not modified (merged annotations are not copied into " +
        "the output).")
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter")
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
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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

    @SetJIPipeDocumentation(name = "Slice range", description = "Only the items within this range are copied into the output. The first index is zero. " +
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
