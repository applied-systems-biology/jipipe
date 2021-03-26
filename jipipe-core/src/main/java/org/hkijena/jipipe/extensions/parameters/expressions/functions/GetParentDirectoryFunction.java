package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;
import java.util.List;

@JIPipeDocumentation(name = "Get parent directory", description = "Returns the parent directory of the provided file/folder path.")
public class GetParentDirectoryFunction extends ExpressionFunction {
    public GetParentDirectoryFunction() {
        super("GET_PARENT_DIRECTORY", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        String pathString = StringUtils.nullToEmpty(parameters.get(0));
        try {
            return Paths.get(pathString).getParent().toString();
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
