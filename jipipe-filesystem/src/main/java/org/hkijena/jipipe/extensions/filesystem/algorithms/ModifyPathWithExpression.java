package org.hkijena.jipipe.extensions.filesystem.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.FileSystemNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeSingleDataBatch;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettingsVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@JIPipeDocumentation(name = "Modify path (expression)", description = "Processes each incoming path with an expression")
@JIPipeNode(nodeTypeCategory = FileSystemNodeTypeCategory.class, menuPath = "Modify")
@JIPipeInputSlot(value = PathData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = PathData.class, slotName = "Output", autoCreate = true)
public class ModifyPathWithExpression extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter expression = new DefaultExpressionParameter("path");

    public ModifyPathWithExpression(JIPipeNodeInfo info) {
        super(info);
    }

    public ModifyPathWithExpression(ModifyPathWithExpression other) {
        super(other);
        this.expression = new DefaultExpressionParameter(other.expression);
    }

    @Override
    protected void runIteration(JIPipeSingleDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Path path = dataBatch.getInputData(getFirstInputSlot(), PathData.class, progressInfo).toPath();

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        variables.putProjectDirectories(getProjectDirectory(), getProjectDataDirs());
        variables.set("path", StringUtils.nullToEmpty(path));

        Object result = expression.evaluate(variables);
        if (result != null) {
            Path outputPath = Paths.get(StringUtils.nullToEmpty(result));
            dataBatch.addOutputData(getFirstOutputSlot(), new PathData(outputPath), progressInfo);
        }
    }

    @JIPipeDocumentation(name = "Expression", description = "Expression that processes the path. Should return a new path. If 'null' is returned, the path will be filtered out.")
    @JIPipeParameter("expression")
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(name = "Input path", key = "path", description = "The input path")
    @ExpressionParameterSettingsVariable(name = "Project directory", description = "The project directory (if available; will be the same as the data directory otherwise)", key = "project_dir")
    @ExpressionParameterSettingsVariable(name = "Project data directories", description = "The user-configured project data directories as map. Access entries by the key.", key = "project_data_dirs")
    public DefaultExpressionParameter getExpression() {
        return expression;
    }

    @JIPipeParameter("expression")
    public void setExpression(DefaultExpressionParameter expression) {
        this.expression = expression;
    }
}
