package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Combine paths", description = "Combines all parameters into a path.")
public class PathCombineFunction extends ExpressionFunction {
    public PathCombineFunction() {
        super("PATH_COMBINE", 1, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        List<String> items = new ArrayList<>();
        for (Object parameter : parameters) {
            String item = StringUtils.nullToEmpty(parameter);
            items.add(item);
        }
        return Paths.get(String.join("/", items));
    }
}
