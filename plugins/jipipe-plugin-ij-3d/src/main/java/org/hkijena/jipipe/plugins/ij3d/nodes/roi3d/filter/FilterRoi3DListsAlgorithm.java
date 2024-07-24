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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter;

import mcib3d.geom.Vector3D;
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
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.BooleanParameterSettings;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Filter 3D ROI list", description = "Only passes 3D ROI lists that match the filter criteria.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "Output", create = true)
public class FilterRoi3DListsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("count > 0");
    private boolean includeAnnotations = true;
    private boolean outputEmptyLists = true;

    public FilterRoi3DListsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterRoi3DListsAlgorithm(FilterRoi3DListsAlgorithm other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
        this.includeAnnotations = other.includeAnnotations;
        this.outputEmptyLists = other.outputEmptyLists;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo);

        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap();
        if (includeAnnotations) {
            for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
                parameters.set(annotation.getName(), annotation.getValue());
            }
        }
        Vector3D[] bounds = rois.getBounds();
        parameters.set("count", rois.size());
        parameters.set("x", bounds[0].x);
        parameters.set("y", bounds[0].y);
        parameters.set("z", bounds[0].z);
        parameters.set("width", bounds[1].x);
        parameters.set("height", bounds[1].y);
        parameters.set("depth", bounds[1].z);
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

    @SetJIPipeDocumentation(name = "Include annotations", description = "If enabled, annotations are also available as string variables. Please note that " +
            "ROI-list specific variables will overwrite annotations with the same name.")
    @JIPipeParameter("include-annotations")
    public boolean isIncludeAnnotations() {
        return includeAnnotations;
    }

    @JIPipeParameter("include-annotations")
    public void setIncludeAnnotations(boolean includeAnnotations) {
        this.includeAnnotations = includeAnnotations;
    }

    @SetJIPipeDocumentation(name = "Filter", description = "The filter expression used to test ROI lists. Must return a boolean.")
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
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("z", "Bounding box Z", "Top-left Z coordinate of the bounding box around all ROIs (zero if empty list)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Bounding box width", "Width of the bounding box around all ROIs (zero if empty list)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Bounding box height", "Height of the bounding box around all ROIs (zero if empty list)"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("depth", "Bounding box depth", "Depth of the bounding box around all ROIs (zero if empty list)"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
