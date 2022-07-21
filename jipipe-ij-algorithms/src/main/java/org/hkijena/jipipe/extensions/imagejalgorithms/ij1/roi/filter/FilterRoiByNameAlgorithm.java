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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi.filter;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

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

    private DefaultExpressionParameter filters = new DefaultExpressionParameter();
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
        this.filters = new DefaultExpressionParameter(other.filters);
        this.outputEmptyLists = other.outputEmptyLists;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData inputRois = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);
        ROIListData outputRois = new ROIListData();

        ExpressionVariables parameters = new ExpressionVariables();
        parameters.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Roi roi : inputRois) {
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
            dataBatch.addOutputData(getFirstOutputSlot(), outputRois, progressInfo);
        }
    }

    @JIPipeParameter("filter")
    @JIPipeDocumentation(name = "Filter", description = "Filtering expression. This is applied per ROI. " +
            "Click the 'f' button to see all available variables you can test.")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getFilters() {
        return filters;
    }

    @JIPipeParameter("filter")
    public void setFilters(DefaultExpressionParameter filters) {
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

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Name", "Name of the ROI (can be empty)", "name"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box X", "Top-left X coordinate of the bounding box around the ROI", "x"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box Y", "Top-left Y coordinate of the bounding box around around the ROI", "y"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box width", "Width of the bounding box around around the ROI", "width"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box height", "Height of the bounding box around around the ROI", "height"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
