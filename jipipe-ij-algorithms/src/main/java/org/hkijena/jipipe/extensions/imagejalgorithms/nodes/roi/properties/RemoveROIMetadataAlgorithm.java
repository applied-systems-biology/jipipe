package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi.properties;

import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Remove/Filter ROI metadata", description = "Allows to filter/remove specific ROI metadata entries")
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ROIListData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
public class RemoveROIMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter filterExpression = new DefaultExpressionParameter("true");

    public RemoveROIMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveROIMetadataAlgorithm(RemoveROIMetadataAlgorithm other) {
        super(other);
        this.filterExpression = new DefaultExpressionParameter(other.filterExpression);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ROIListData rois = new ROIListData(dataBatch.getInputData(getFirstInputSlot(), ROIListData.class, progressInfo));
        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        for (Roi roi : rois) {
            Map<String, String> map = ImageJUtils.getRoiProperties(roi);
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
            ImageJUtils.setRoiProperties(roi, map);
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
