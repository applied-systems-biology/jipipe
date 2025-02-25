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

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Filter 2D ROI list", description = "Only passes ROI lists that match the filter criteria.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROI2DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Output", create = true)
public class FilterROIListsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("count > 0");
    private boolean outputEmptyLists = true;

    public FilterROIListsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterROIListsAlgorithm(FilterROIListsAlgorithm other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
        this.outputEmptyLists = other.outputEmptyLists;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI2DListData rois = iterationStep.getInputData(getFirstInputSlot(), ROI2DListData.class, progressInfo);

        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap(iterationStep);

        Rectangle bounds = rois.getBounds();
        parameters.set("count", rois.size());
        parameters.set("x", bounds.x);
        parameters.set("y", bounds.y);
        parameters.set("width", bounds.width);
        parameters.set("height", bounds.height);
        if (filter.test(parameters)) {
            iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
        } else if (outputEmptyLists) {
            iterationStep.addOutputData(getFirstOutputSlot(), new ROI2DListData(), progressInfo);
        }
    }

    @SetJIPipeDocumentation(name = "Operation on filtered-out ROI", description = "Determines what kind of data is stored into the output if a ROI list was filtered out.")
    @JIPipeParameter("output-empty-list")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Output empty list", falseLabel = "Output nothing")
    public boolean isOutputEmptyLists() {
        return outputEmptyLists;
    }

    @JIPipeParameter("output-empty-list")
    public void setOutputEmptyLists(boolean outputEmptyLists) {
        this.outputEmptyLists = outputEmptyLists;
    }

    @SetJIPipeDocumentation(name = "Keep ROI list if", description = "The filter expression used to test ROI lists. Must return a boolean.")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI list")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    public static class VariablesInfo implements JIPipeExpressionVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("count", "Number of items", "Number of items in the list"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "Bounding box X", "Top-left X coordinate of the bounding box around all ROIs (zero if empty list)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("y", "Bounding box Y", "Top-left Y coordinate of the bounding box around all ROIs (zero if empty list)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Bounding box width", "Width of the bounding box around all ROIs (zero if empty list)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Bounding box height", "Height of the bounding box around all ROIs (zero if empty list)"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
