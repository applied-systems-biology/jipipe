package org.hkijena.jipipe.extensions.strings.nodes.json;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.hkijena.jipipe.utils.StringUtils;

@JIPipeDocumentation(name = "Extract text from JSON", description = "Extracts a value from the input JSON data (via JsonPath). " +
        "Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath")
@JIPipeCitation("JsonPath: https://goessner.net/articles/JsonPath/")
@JIPipeNode(menuPath = "JSON", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = JsonData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = StringData.class, slotName = "Output", autoCreate = true)
public class ExtractTextFromJsonAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter jsonPath = new DefaultExpressionParameter("\"$\"");

    public ExtractTextFromJsonAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractTextFromJsonAlgorithm(ExtractTextFromJsonAlgorithm other) {
        super(other);
    }


    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        JsonData data = iterationStep.getInputData(getFirstInputSlot(), JsonData.class, progressInfo);
        DocumentContext documentContext = JsonPath.parse(data.getData());

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        String path = jsonPath.evaluateToString(variables);
        String value = StringUtils.nullToEmpty(documentContext.read(path));

        iterationStep.addOutputData(getFirstOutputSlot(), new StringData(value), progressInfo);
    }

    @JIPipeDocumentation(name = "JSON path", description = "An expression that returns the JsonPath of the JSON entries. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
    @JIPipeParameter(value = "json-path")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    public DefaultExpressionParameter getJsonPath() {
        return jsonPath;
    }

    @JIPipeParameter("json-path")
    public void setJsonPath(DefaultExpressionParameter jsonPath) {
        this.jsonPath = jsonPath;
    }
}
