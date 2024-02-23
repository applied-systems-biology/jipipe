/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.filter;

import ij.gui.Roi;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Slice ROI list", description = "Extracts a sublist of ROI from the input")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "ROI", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class SliceROIListAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntegerRange selectedIndices = new IntegerRange();

    private boolean autoClamp = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public SliceROIListAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public SliceROIListAlgorithm(SliceROIListAlgorithm other) {
        super(other);
        this.selectedIndices = new IntegerRange(other.selectedIndices);
        this.autoClamp = other.autoClamp;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ROIListData inputRois = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

        ROIListData outputRois = new ROIListData();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        List<Integer> indices = selectedIndices.getIntegers(0, inputRois.size() - 1, variables);
        for (int roiIndex : indices) {
            if (autoClamp && (roiIndex < 0 || roiIndex >= inputRois.size())) {
                continue;
            }
            Roi roi = inputRois.get(roiIndex);
            outputRois.add(roi);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Selected indices", description = "The output will only contain the ROI indices specified in by the value. For example, the value '0' returns the first ROI. A value of '0-4' returns the top 5 items.")
    @JIPipeParameter("selected-indices")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public IntegerRange getSelectedIndices() {
        return selectedIndices;
    }

    @JIPipeParameter("selected-indices")
    public void setSelectedIndices(IntegerRange selectedIndices) {
        this.selectedIndices = selectedIndices;
    }

    @SetJIPipeDocumentation(name = "Ignore missing items", description = "If enabled, there will be not error if you select too many items (e.g. if the list only contains " +
            "10 items, but you select the top 20)")
    @JIPipeParameter("auto-clamp")
    public boolean isAutoClamp() {
        return autoClamp;
    }

    @JIPipeParameter("auto-clamp")
    public void setAutoClamp(boolean autoClamp) {
        this.autoClamp = autoClamp;
    }
}
