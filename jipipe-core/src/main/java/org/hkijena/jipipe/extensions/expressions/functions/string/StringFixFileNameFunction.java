package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@SetJIPipeDocumentation(name = "Fix file name", description = "Replaces characters not allowed in file names with spaces and limits the length to 255 characters. " +
        "Please note that colons and slashes that are used in paths are also replaced.")
public class StringFixFileNameFunction extends ExpressionFunction {

    public StringFixFileNameFunction() {
        super("STRING_FIX_FILE_NAME", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String fileName = StringUtils.nullToEmpty(parameters.get(0));
        return StringUtils.makeFilesystemCompatible(fileName);
    }
}
