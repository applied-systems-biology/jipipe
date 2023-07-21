package org.hkijena.jipipe.extensions.strings.nodes.text;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.strings.StringData;

@JIPipeDocumentation(name = "Process text (expression)", description = "Processes text with an expression.")
@JIPipeNode(menuPath = "Text", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = StringData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
public class ProcessTextDataAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter textProcessor = new DefaultExpressionParameter("text");

    public ProcessTextDataAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ProcessTextDataAlgorithm(ProcessTextDataAlgorithm other) {
        super(other);
        this.textProcessor = new DefaultExpressionParameter(other.textProcessor);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        String inputData = dataBatch.getInputData(getFirstInputSlot(), StringData.class, progressInfo).getData();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        variables.set("text", inputData);
        String output = getTextProcessor().evaluateToString(variables);

        dataBatch.addOutputData(getFirstOutputSlot(), new StringData(output), progressInfo);
    }

    @JIPipeDocumentation(name = "Text processor", description = "An expression that allows to process the text.")
    @JIPipeParameter(value = "text-processor")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Text", key = "text", description = "The input text")
    public DefaultExpressionParameter getTextProcessor() {
        return textProcessor;
    }

    @JIPipeParameter("text-processor")
    public void setTextProcessor(DefaultExpressionParameter textProcessor) {
        this.textProcessor = textProcessor;
    }

}
