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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.metadata;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SetJIPipeDocumentation(name = "Remove/Filter image metadata", description = "Allows to filter/remove specific image metadata entries")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Metadata")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Output", create = true)
public class RemoveImageMetadataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filterExpression = new JIPipeExpressionParameter("true");

    public RemoveImageMetadataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public RemoveImageMetadataAlgorithm(RemoveImageMetadataAlgorithm other) {
        super(other);
        this.filterExpression = new JIPipeExpressionParameter(other.filterExpression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        Map<String, String> map = ImageJUtils.getImageProperties(imagePlus);
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
        ImageJUtils.setImageProperties(imagePlus, map);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imagePlus), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Filter", description = "This expression should return <code>true</code> if the property should be removed")
    @JIPipeParameter("filter")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "key", name = "Metadata key", description = "The name of the metadata")
    @AddJIPipeExpressionParameterVariable(key = "value", name = "Metadata value", description = "The value of the metadata")
    @AddJIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Other image metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getFilterExpression() {
        return filterExpression;
    }

    @JIPipeParameter("filter")
    public void setFilterExpression(JIPipeExpressionParameter filterExpression) {
        this.filterExpression = filterExpression;
    }
}
