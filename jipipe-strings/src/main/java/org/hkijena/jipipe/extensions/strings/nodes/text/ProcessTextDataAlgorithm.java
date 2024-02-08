package org.hkijena.jipipe.extensions.strings.nodes.text;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.strings.StringData;

@JIPipeDocumentation(name = "Process text (expression)", description = "Processes text with an expression.")
@JIPipeNode(menuPath = "Text", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = StringData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
public class ProcessTextDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter textProcessor = new JIPipeExpressionParameter("text");

    public ProcessTextDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ProcessTextDataAlgorithm(ProcessTextDataAlgorithm other) {
        super(other);
        this.textProcessor = new JIPipeExpressionParameter(other.textProcessor);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        String inputData = iterationStep.getInputData(getFirstInputSlot(), StringData.class, progressInfo).getData();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        variables.set("text", inputData);
        String output = getTextProcessor().evaluateToString(variables);

        iterationStep.addOutputData(getFirstOutputSlot(), new StringData(output), progressInfo);
    }

    @JIPipeDocumentation(name = "Text processor", description = "An expression that allows to process the text.")
    @JIPipeParameter(value = "text-processor")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Text", key = "text", description = "The input text")
    public JIPipeExpressionParameter getTextProcessor() {
        return textProcessor;
    }

    @JIPipeParameter("text-processor")
    public void setTextProcessor(JIPipeExpressionParameter textProcessor) {
        this.textProcessor = textProcessor;
    }

}
