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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.metadata;

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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SetJIPipeDocumentation(name = "Remove/Filter 3D ROI metadata", description = "Allows to filter/remove specific ROI metadata entries")
@ConfigureJIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ROI3DListData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI3DListData.class, name = "Output", create = true)
public class RemoveROI3DMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filterExpression = new JIPipeExpressionParameter("true");

    public RemoveROI3DMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveROI3DMetadataAlgorithm(RemoveROI3DMetadataAlgorithm other) {
        super(other);
        this.filterExpression = new JIPipeExpressionParameter(other.filterExpression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ROI3DListData rois = new ROI3DListData(iterationStep.getInputData(getFirstInputSlot(), ROI3DListData.class, progressInfo));
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap(iterationStep);

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
        iterationStep.addOutputData(getFirstOutputSlot(), rois, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Remove ROI if", description = "This expression is executed per ROI property and should return <code>true</code> if the property should be removed")
    @JIPipeParameter("filter")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "key", name = "Metadata key", description = "The name of the metadata")
    @AddJIPipeExpressionParameterVariable(key = "value", name = "Metadata value", description = "The value of the metadata")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Other ROI metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getFilterExpression() {
        return filterExpression;
    }

    @JIPipeParameter("filter")
    public void setFilterExpression(JIPipeExpressionParameter filterExpression) {
        this.filterExpression = filterExpression;
    }
}
