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

package org.hkijena.jipipe.plugins.utils.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;

import java.util.HashSet;
import java.util.List;

@SetJIPipeDocumentation(name = "Limit data", description = "Slices the incoming data table into a smaller table with a limited row set according to the selected row.")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "Filter")
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
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        HashSet<Integer> indices = new HashSet<>(sliceRange.getIntegers(0, getFirstInputSlot().getRowCount(), new JIPipeExpressionVariablesMap(this)));
        for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
            if (indices.contains(row)) {
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

    @SetJIPipeDocumentation(name = "Slice range", description = "Only the data rows within this range are copied into the output. The first index is zero. " +
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
