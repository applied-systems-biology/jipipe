package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@JIPipeDocumentation(name = "Path exists", description = "Returns true if the provided string is a path that exists, otherwise false.")
public class PathExistsFunction extends ExpressionFunction {
    public PathExistsFunction() {
        super("PATH_EXISTS", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
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
