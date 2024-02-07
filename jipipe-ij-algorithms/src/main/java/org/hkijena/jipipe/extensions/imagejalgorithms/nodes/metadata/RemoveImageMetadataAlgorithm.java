package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.metadata;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Remove/Filter image metadata", description = "Allows to filter/remove specific image metadata entries")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Metadata")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getDuplicateImage();
        ExpressionVariables variables = new ExpressionVariables();
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

    @JIPipeDocumentation(name = "Filter", description = "This expression should return <code>true</code> if the property should be removed")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "key", name = "Metadata key", description = "The name of the metadata")
    @JIPipeExpressionParameterVariable(key = "value", name = "Metadata value", description = "The value of the metadata")
    @JIPipeExpressionParameterVariable(name = "metadata.<Metadata key>", description = "Other image metadata/properties accessible via their string keys")
    public JIPipeExpressionParameter getFilterExpression() {
        return filterExpression;
    }

    @JIPipeParameter("filter")
    public void setFilterExpression(JIPipeExpressionParameter filterExpression) {
        this.filterExpression = filterExpression;
    }
}
