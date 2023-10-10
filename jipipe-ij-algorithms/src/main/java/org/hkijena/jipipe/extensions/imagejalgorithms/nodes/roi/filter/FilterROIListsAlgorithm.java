package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.filter;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Filter ROI list", description = "Only passes ROI lists that match the filter criteria.")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class FilterROIListsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filter = new DefaultExpressionParameter("count > 0");
    private boolean includeAnnotations = true;
    private boolean outputEmptyLists = true;

    public FilterROIListsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterROIListsAlgorithm(FilterROIListsAlgorithm other) {
        super(other);
        this.filter = new DefaultExpressionParameter(other.filter);
        this.includeAnnotations = other.includeAnnotations;
        this.outputEmptyLists = other.outputEmptyLists;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData rois = dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo);

        ExpressionVariables parameters = new ExpressionVariables();
        if (includeAnnotations) {
            for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
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
            dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
        } else if (outputEmptyLists) {
            dataBatch.addOutputData(getFirstOutputSlot(), new ROIListData(), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Operation on filtered-out ROI", description = "Determines what kind of data is stored into the output if a ROI list was filtered out.")
    @JIPipeParameter("output-empty-list")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Output empty list", falseLabel = "Output nothing")
    public boolean isOutputEmptyLists() {
        return outputEmptyLists;
    }

    @JIPipeParameter("output-empty-list")
    public void setOutputEmptyLists(boolean outputEmptyLists) {
        this.outputEmptyLists = outputEmptyLists;
    }

    @JIPipeDocumentation(name = "Include annotations", description = "If enabled, annotations are also available as string variables. Please note that " +
            "ROI-list specific variables will overwrite annotations with the same name.")
    @JIPipeParameter("include-annotations")
    public boolean isIncludeAnnotations() {
        return includeAnnotations;
    }

    @JIPipeParameter("include-annotations")
    public void setIncludeAnnotations(boolean includeAnnotations) {
        this.includeAnnotations = includeAnnotations;
    }

    @JIPipeDocumentation(name = "Filter", description = "The filter expression used to test ROI lists. Must return a boolean.")
    @JIPipeParameter("filter")
    @ExpressionParameterSettings(variableSource = VariableSource.class, hint = "per ROI list")
    public DefaultExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(DefaultExpressionParameter filter) {
        this.filter = filter;
    }

    public static class VariableSource implements ExpressionParameterVariableSource {

        public static final Set<ExpressionParameterVariable> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("Number of items", "Number of items in the list", "count"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box X", "Top-left X coordinate of the bounding box around all ROIs (zero if empty list)", "x"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box Y", "Top-left Y coordinate of the bounding box around all ROIs (zero if empty list)", "y"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box width", "Width of the bounding box around all ROIs (zero if empty list)", "width"));
            VARIABLES.add(new ExpressionParameterVariable("Bounding box height", "Height of the bounding box around all ROIs (zero if empty list)", "height"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
