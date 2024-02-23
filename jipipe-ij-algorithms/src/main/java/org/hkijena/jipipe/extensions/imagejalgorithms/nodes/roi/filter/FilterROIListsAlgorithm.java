package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.filter;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
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
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Filter ROI list", description = "Only passes ROI lists that match the filter criteria.")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = ROIListData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ROIListData.class, slotName = "Output", create = true)
public class FilterROIListsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("count > 0");
    private boolean includeAnnotations = true;
    private boolean outputEmptyLists = true;

    public FilterROIListsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterROIListsAlgorithm(FilterROIListsAlgorithm other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
        this.includeAnnotations = other.includeAnnotations;
        this.outputEmptyLists = other.outputEmptyLists;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROIListData rois = iterationStep.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

        JIPipeExpressionVariablesMap parameters = new JIPipeExpressionVariablesMap();
        if (includeAnnotations) {
            for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
                parameters.set(annotation.getName(), annotation.getValue());
            }
        }
        Rectangle bounds = rois.getBounds();
        parameters.set("count", rois.size());
        parameters.set("x", bounds.x);
        parameters.set("y", bounds.y);
        parameters.set("width", bounds.width);
        parameters.set("height", bounds.height);
        if (filter.test(parameters)) {
            iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
        } else if (outputEmptyLists) {
            iterationStep.addOutputData(getFirstOutputSlot(), new ROIListData(), progressInfo);
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

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

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
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
