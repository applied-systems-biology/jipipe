package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@JIPipeDocumentation(name = "Path is file", description = "Returns true if the provided string is a path that points to a directory, otherwise false.")
public class IsFileFunction extends ExpressionFunction {
    public IsFileFunction() {
        super("IS_FILE", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        String pathString = StringUtils.nullToEmpty(parameters.get(0));
        try {
            return Files.isRegularFile(Paths.get(pathString));
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
