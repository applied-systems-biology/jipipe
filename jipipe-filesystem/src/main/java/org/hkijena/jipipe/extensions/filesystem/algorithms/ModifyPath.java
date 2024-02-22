package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.DefineJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.JIPipeProjectDirectoriesVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.PathQueryExpression;
import org.hkijena.jipipe.extensions.expressions.variables.PathFilterExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@SetJIPipeDocumentation(name = "Modify path", description = "Uses an expression to modify a path.")
@AddJIPipeInputSlot(value = PathData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = PathData.class, slotName = "Output", create = true)
@DefineJIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)
public class ModifyPath extends JIPipeSimpleIteratingAlgorithm {

    private PathQueryExpression expression = new PathQueryExpression("path");
    private boolean accessAnnotations = true;

    public ModifyPath(JIPipeNodeInfo info) {
        super(info);
    }

    public ModifyPath(ModifyPath other) {
        super(other);
        this.expression = new PathQueryExpression(other.expression);
        this.accessAnnotations = other.accessAnnotations;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        PathData input = iterationStep.getInputData(getFirstInputSlot(), PathData.class, progressInfo);
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        if (accessAnnotations) {
            for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
                variables.set(annotation.getName(), annotation.getValue());
            }
        }
        variables.putProjectDirectories(getProjectDirectory(), getProjectDataDirs());

        PathFilterExpressionParameterVariablesInfo.buildFor(input.toPath(), variables);
        Object result = expression.evaluate(variables);
        if (result instanceof String) {
            iterationStep.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(StringUtils.nullToEmpty(result))), progressInfo);
        } else if (result instanceof Path) {
            iterationStep.addOutputData(getFirstOutputSlot(), new PathData((Path) result), progressInfo);
        } else {
            progressInfo.log("Expression generated value '" + result + "', which is not a string. Dropping this data.");
        }
    }

    @SetJIPipeDocumentation(name = "Expression", description = "Expression that is used to modify the path. " +
            "Available variables include path, absolute_path, name, and parent and are passed as strings. " +
            "Additionally, annotations are available as variables if enabled.\n\nIf the expression returns a non-string value, the path data will be skipped.\n\n" +
            "To improve compatibility between operating systems, we recommend to use '/' as path separator.")
    @JIPipeParameter("expression")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeProjectDirectoriesVariablesInfo.class)
    public PathQueryExpression getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(PathQueryExpression expression) {
        this.expression = expression;
    }

    @SetJIPipeDocumentation(name = "Annotations are variables", description = "If enabled, the expression has variables that contain " +
            "annotation values. Annotations with one of the names path, absolute_path, name, or parent are overridden by the input path properties.")
    @JIPipeParameter("access-annotations")
    public boolean isAccessAnnotations() {
        return accessAnnotations;
    }

    @JIPipeParameter("access-annotations")
    public void setAccessAnnotations(boolean accessAnnotations) {
        this.accessAnnotations = accessAnnotations;
    }
}
