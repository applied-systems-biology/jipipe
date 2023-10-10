package org.hkijena.jipipe.extensions.ij3d.nodes.roi3d.metadata;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Remove/Filter 3D ROI metadata", description = "Allows to filter/remove specific ROI metadata entries")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ROI3DListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
public class RemoveROI3DMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filterExpression = new DefaultExpressionParameter("true");

    public RemoveROI3DMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveROI3DMetadataAlgorithm(RemoveROI3DMetadataAlgorithm other) {
        super(other);
        this.filterExpression = new DefaultExpressionParameter(other.filterExpression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = new ROI3DListData(dataBatch.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo));
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (ROI3D roi : rois) {
            Map<String, String> map = roi.getMetadata();
            Set<String> toRemove = new HashSet<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                variables.set("key", entry.getKey());
                variables.set("value", entry.getValue());
                if (filterExpression.evaluateToBoolean(variables)) {
                    toRemove.add(entry.getKey());
                }
            }
            for (String key : toRemove) {
                map.remove(key);
            }
            roi.setMetadata(map);
        }
        dataBatch.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @JIPipeDocumentation(name = "Filter", description = "This expression is executed per ROI property and should return <code>true</code> if the property should be removed")
    @JIPipeParameter("filter")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "key", name = "Metadata key", description = "The name of the metadata")
    @ExpressionParameterSettingsVariable(key = "value", name = "Metadata value", description = "The value of the metadata")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Other ROI metadata/properties accessible via their string keys")
    public DefaultExpressionParameter getFilterExpression() {
        return filterExpression;
    }

    @JIPipeParameter("filter")
    public void setFilterExpression(DefaultExpressionParameter filterExpression) {
        this.filterExpression = filterExpression;
    }
}
