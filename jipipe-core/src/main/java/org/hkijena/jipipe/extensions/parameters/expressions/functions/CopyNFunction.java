package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionEvaluator;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Copy N times", description = "Copies the left operand N times, as defined by the right operand. " +
        "If N == 0, NULL is returned. If N == 1, the left operand is returned. If N > 1, an array of N copies is returned.")
public class CopyNFunction extends ExpressionFunction {
    public CopyNFunction() {
        super("COPY_N", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if(index == 0) {
            return new ParameterInfo("Object", "The object to be copied");
        }
        else {
            return new ParameterInfo("Copies", "Number of copies to make", Number.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        Object source = parameters.get(0);
        int n = ((Number)parameters.get(1)).intValue();

        if(n <= 0)
            return null;
        if(n == 1)
            return source;
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
           result.add(DefaultExpressionEvaluator.deepCopyObject(source));
        }
        return result;
    }
}
