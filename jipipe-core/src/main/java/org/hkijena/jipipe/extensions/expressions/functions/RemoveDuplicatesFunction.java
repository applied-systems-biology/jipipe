package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@JIPipeDocumentation(name = "Remove duplicates", description = "Removes duplicate elements from arrays")
public class RemoveDuplicatesFunction extends ExpressionFunction {

    public RemoveDuplicatesFunction() {
        super("REMOVE_DUPLICATES", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("array", "The array", Collection.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return new HashSet<>((Collection<?>) parameters.get(0));
    }
}
