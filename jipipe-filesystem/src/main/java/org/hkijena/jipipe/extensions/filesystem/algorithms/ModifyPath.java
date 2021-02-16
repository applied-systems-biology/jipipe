package org.hkijena.jipipe.extensions.filesystem.algorithms;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.parameters.expressions.PathQueryExpression;
import org.hkijena.jipipe.extensions.parameters.expressions.variables.PathFilterExpressionParameterVariableSource;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;

@JIPipeDocumentation(name = "Modify path", description = "Uses an expression to modify a path.")
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
@JIPipeOrganization(menuPath = "Modify", nodeTypeCategory = FileSystemNodeTypeCategory.class)
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
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        if(accessAnnotations) {
            for (JIPipeAnnotation annotation : dataBatch.getAnnotations().values()) {
                variableSet.set(annotation.getName(), annotation.getValue());
            }
        }
        PathFilterExpressionParameterVariableSource.buildFor(input.toPath(), variableSet);
        Object result = expression.evaluate(variableSet);
        if(result instanceof String) {
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(Paths.get(StringUtils.nullToEmpty(result))), progressInfo);
        }
        else {
            progressInfo.log("Expression generated value '" + result + "', which is not a string. Dropping this data.");
        }
    }

    @JIPipeDocumentation(name = "Expression", description = "Expression that is used to modify the path. " +
            "Available variables include path, absolute_path, name, and parent and are passed as strings. " +
            "Additionally, annotations are available as variables if enabled.\n\nIf the expression returns a non-string value, the path data will be skipped.\n\n" +
            "To improve compatibility between operating systems, we recommend to use '/' as path separator.")
    @JIPipeParameter("expression")
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
