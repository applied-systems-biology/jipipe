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
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper around {@link ij.plugin.frame.RoiManager}
 */
@JIPipeDocumentation(name = "Filter ROI by properties (expression)", description = "Filters the ROI list elements by the name and other basic properties.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class FilterRoiByNameAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filters = new JIPipeExpressionParameter();
    private boolean outputEmptyLists = true;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public FilterRoiByNameAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public FilterRoiByNameAlgorithm(FilterRoiByNameAlgorithm other) {
        super(other);
        this.filters = new JIPipeExpressionParameter(other.filters);
        this.outputEmptyLists = other.outputEmptyLists;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData inputRois = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        ROIListData outputRois = new ROIListData();

        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap();
        parameters.putAnnotations(iterationStep.getMergedTextAnnotations());
        for (int i = 0; i < inputRois.size(); i++) {
            Roi roi = inputRois.get(i);
            parameters.set("num_roi", inputRois.size());
            parameters.set("index", i);
            parameters.set("name", StringUtils.nullToEmpty(roi.getName()));
            parameters.set("x", roi.getBounds().x);
            parameters.set("y", roi.getBounds().y);
            parameters.set("width", roi.getBounds().width);
            parameters.set("height", roi.getBounds().height);
            if (filters.test(parameters)) {
                outputRois.add(roi);
            }
        }

        if (!outputRois.isEmpty() || outputEmptyLists) {
            iterationStep.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
        }
    }

    @JIPipeParameter("filter")
    @JIPipeDocumentation(name = "Filter", description = "Filtering expression. This is applied per ROI. " +
            "Click the 'f' button to see all available variables you can test.")
    @JIPipeExpressionParameterSettings(variableSource = VariablesInfo.class, hint = "per ROI")
    public JIPipeExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(JIPipeExpressionParameter filters) {
        this.filters = filters;
    }

    @JIPipeDocumentation(name = "Output empty lists", description = "If enabled, the node will also output empty lists.")
    @JIPipeParameter("output-empty-lists")
    public boolean isOutputEmptyLists() {
        return outputEmptyLists;
    }

    @JIPipeParameter("output-empty-lists")
    public void setOutputEmptyLists(boolean outputEmptyLists) {
        this.outputEmptyLists = outputEmptyLists;
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("name", "Name", "Name of the ROI (can be empty)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("index", "Index", "Index of the ROI (First value is zero)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_roi", "Number of ROI", "Number of ROI in the ROI list"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "Bounding box X", "Top-left X coordinate of the bounding box around the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("y", "Bounding box Y", "Top-left Y coordinate of the bounding box around around the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Bounding box width", "Width of the bounding box around around the ROI"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Bounding box height", "Height of the bounding box around around the ROI"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
