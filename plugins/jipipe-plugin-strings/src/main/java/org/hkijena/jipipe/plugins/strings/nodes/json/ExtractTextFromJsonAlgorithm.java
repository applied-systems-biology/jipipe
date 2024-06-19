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

package org.hkijena.jipipe.plugins.strings.nodes.json;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.hkijena.jipipe.api.AddJIPipeCitation;
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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.strings.JsonData;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.utils.StringUtils;

@SetJIPipeDocumentation(name = "Extract text from JSON", description = "Extracts a value from the input JSON data (via JsonPath). " +
        "Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath")
@AddJIPipeCitation("JsonPath: https://goessner.net/articles/JsonPath/")
@ConfigureJIPipeNode(menuPath = "JSON", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JsonData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = StringData.class, slotName = "Output", create = true)
public class ExtractTextFromJsonAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter jsonPath = new JIPipeExpressionParameter("\"$\"");

    public ExtractTextFromJsonAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExtractTextFromJsonAlgorithm(ExtractTextFromJsonAlgorithm other) {
        super(other);
    }


    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JsonData data = iterationStep.getInputData(getFirstInputSlot(), JsonData.class, progressInfo);
        DocumentContext documentContext = JsonPath.parse(data.getData());

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());

        String path = jsonPath.evaluateToString(variables);
        String value = StringUtils.nullToEmpty(documentContext.read(path));

        iterationStep.addOutputData(getFirstOutputSlot(), new StringData(value), progressInfo);
    }

    @SetJIPipeDocumentation(name = "JSON path", description = "An expression that returns the JsonPath of the JSON entries. Please visit https://goessner.net/articles/JsonPath/ to learn more about JsonPath.")
    @JIPipeParameter(value = "json-path")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getJsonPath() {
        return jsonPath;
    }

    @JIPipeParameter("json-path")
    public void setJsonPath(JIPipeExpressionParameter jsonPath) {
        this.jsonPath = jsonPath;
    }
}
