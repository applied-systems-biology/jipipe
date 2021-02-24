package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

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
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        return new HashSet<>((Collection<?>) parameters.get(0));
    }
}
