package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.JIPipeProjectDirectoriesVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@JIPipeDocumentation(name = "Modify path (expression)", description = "Processes each incoming path with an expression")
@JIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
public class ModifyPathWithExpression extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter expression = new JIPipeExpressionParameter("path");

    public ModifyPathWithExpression(JIPipeNodeInfo info) {
        super(info);
    }

    public ModifyPathWithExpression(ModifyPathWithExpression other) {
        super(other);
        this.expression = new JIPipeExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Path path = iterationStep.getInputData(getFirstInputSlot(), PathData.class, progressInfo).toPath();

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        variables.putProjectDirectories(getProjectDirectory(), getProjectDataDirs());
        variables.set("path", StringUtils.nullToEmpty(path));

        Object result = expression.evaluate(variables);
        if (result != null) {
            Path outputPath = Paths.get(StringUtils.nullToEmpty(result));
            iterationStep.addOutputData(getFirstOutputSlot(), new PathData(outputPath), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Expression", description = "Expression that processes the path. Should return a new path. If 'null' is returned, the path will be filtered out.")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(name = "Input path", key = "path", description = "The input path")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeProjectDirectoriesVariablesInfo.class)
    public JIPipeExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(JIPipeExpressionParameter expression) {
        this.expression = expression;
    }
}
