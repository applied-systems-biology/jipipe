package org.hkijena.jipipe.extensions.expressions.functions.filesystem;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SetJIPipeDocumentation(name = "Path exists", description = "Returns true if the provided string is a path that exists, otherwise false.")
public class PathExistsFunction extends ExpressionFunction {
    public PathExistsFunction() {
        super("PATH_EXISTS", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String pathString = StringUtils.nullToEmpty(parameters.get(0));
        try {
            return Files.exists(Paths.get(pathString));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Path", "String that contains the path", String.class);
        }
        return null;
    }
}
