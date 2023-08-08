package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.PathQueryExpression;
import org.hkijena.jipipe.extensions.expressions.variables.PathFilterExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JIPipeDocumentation(name = "Modify path", description = "Uses an expression to modify a path.")
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        PathData input = dataBatch.getInputData(getFirstInputSlot(), PathData.class, progressInfo);
        ExpressionVariables variables = new ExpressionVariables();
        if (accessAnnotations) {
            for (JIPipeTextAnnotation annotation : dataBatch.getMergedTextAnnotations().values()) {
                variables.set(annotation.getName(), annotation.getValue());
            }
        }
        variables.putProjectDirectories(getProjectDirectory(), getProjectDataDirs());

        PathFilterExpressionParameterVariableSource.buildFor(input.toPath(), variables);
        Object result = expression.evaluate(variables);
        if (result instanceof String) {
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(StringUtils.nullToEmpty(result))), progressInfo);
        } else if (result instanceof Path) {
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData((Path) result), progressInfo);
        } else {
            progressInfo.log("Expression generated value '" + result + "', which is not a string. Dropping this data.");
        }
    }

    @JIPipeDocumentation(name = "Expression", description = "Expression that is used to modify the path. " +
            "Available variables include path, absolute_path, name, and parent and are passed as strings. " +
            "Additionally, annotations are available as variables if enabled.\n\nIf the expression returns a non-string value, the path data will be skipped.\n\n" +
            "To improve compatibility between operating systems, we recommend to use '/' as path separator.")
    @JIPipeParameter("expression")
    @ExpressionParameterSettingsVariable(name = "Project directory", description = "The project directory (if available; will be the same as the data directory otherwise)", key = "project_dir")
    @ExpressionParameterSettingsVariable(name = "Project data directories", description = "The user-configured project data directories as map. Access entries by the key.", key = "project_data_dirs")
    public PathQueryExpression getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(PathQueryExpression expression) {
        this.expression = expression;
    }

    @JIPipeDocumentation(name = "Annotations are variables", description = "If enabled, the expression has variables that contain " +
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
