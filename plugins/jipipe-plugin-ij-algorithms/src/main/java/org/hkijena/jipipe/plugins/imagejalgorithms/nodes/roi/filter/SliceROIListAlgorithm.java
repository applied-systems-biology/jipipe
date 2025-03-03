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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.filter;

import ij.gui.Roi;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;

import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@SetJIPipeDocumentation(name = "Slice 2D ROI list", description = "Extracts a sublist of ROI from the input")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "ROI", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
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

        ROI2DListData inputRois = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);

        ROI2DListData outputRois = new ROI2DListData();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

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
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
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
