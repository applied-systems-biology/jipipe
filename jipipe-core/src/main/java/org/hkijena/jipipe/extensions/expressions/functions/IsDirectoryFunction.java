package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@JIPipeDocumentation(name = "Path is directory", description = "Returns true if the provided string is a path that points to a directory, otherwise false.")
public class IsDirectoryFunction extends ExpressionFunction {
    public IsDirectoryFunction() {
        super("IS_DIRECTORY", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String pathString = StringUtils.nullToEmpty(parameters.get(0));
        try {
            return Files.isDirectory(Paths.get(pathString));
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
