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

package org.hkijena.jipipe.plugins.strings.nodes.text;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.strings.StringData;

@SetJIPipeDocumentation(name = "Process text (expression)", description = "Processes text with an expression.")
@ConfigureJIPipeNode(menuPath = "Text", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = StringData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = StringData.class, name = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        String inputData = iterationStep.getInputData(getFirstInputSlot(), StringData.class, progressInfo).getData();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        variables.set("text", inputData);
        String output = getTextProcessor().evaluateToString(variables);

        iterationStep.addOutputData(getFirstOutputSlot(), new StringData(output), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Text processor", description = "An expression that allows to process the text.")
    @JIPipeParameter(value = "text-processor")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(name = "Text", key = "text", description = "The input text")
    public JIPipeExpressionParameter getTextProcessor() {
        return textProcessor;
    }

    @JIPipeParameter("text-processor")
    public void setTextProcessor(JIPipeExpressionParameter textProcessor) {
        this.textProcessor = textProcessor;
    }

}
