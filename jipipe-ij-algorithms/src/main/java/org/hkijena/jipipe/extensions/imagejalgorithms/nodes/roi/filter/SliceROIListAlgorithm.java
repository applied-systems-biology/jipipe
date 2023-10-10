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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.List;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Slice ROI list", description = "Extracts a sublist of ROI from the input")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "ROI", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

        ROIListData inputRois = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

        ROIListData outputRois = new ROIListData();
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());

        List<Integer> indices = selectedIndices.getIntegers(0, inputRois.size() - 1, variables);
        for (int roiIndex : indices) {
            if (autoClamp && (roiIndex < 0 || roiIndex >= inputRois.size())) {
                continue;
            }
            Roi roi = inputRois.get(roiIndex);
            outputRois.add(roi);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
    }

    @JIPipeDocumentation(name = "Selected indices", description = "The output will only contain the ROI indices specified in by the value. For example, the value '0' returns the first ROI. A value of '0-4' returns the top 5 items.")
    @JIPipeParameter("selected-indices")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public IntegerRange getSelectedIndices() {
        return selectedIndices;
    }

    @JIPipeParameter("selected-indices")
    public void setSelectedIndices(IntegerRange selectedIndices) {
        this.selectedIndices = selectedIndices;
    }

    @JIPipeDocumentation(name = "Ignore missing items", description = "If enabled, there will be not error if you select too many items (e.g. if the list only contains " +
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
