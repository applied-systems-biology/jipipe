package org.hkijena.jipipe.extensions.expressions.functions.filesystem;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;
import java.util.List;

@JIPipeDocumentation(name = "Get file/folder name", description = "Returns the name of the file or folder the path is pointing at.")
public class GetPathNameFunction extends ExpressionFunction {
    public GetPathNameFunction() {
        super("GET_FILE_NAME", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String pathString = StringUtils.nullToEmpty(parameters.get(0));
        try {
            return Paths.get(pathString).getFileName().toString();
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