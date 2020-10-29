package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.List;

@JIPipeDocumentation(name = "If-Else condition", description = "Returns the second parameter if the first is true, otherwise return the third parameter.")
public class IfElseFunction extends ExpressionFunction {

    public IfElseFunction() {
        super("IF_ELSE", 3);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        boolean condition = (boolean) parameters.get(0);
        if(condition)
            return parameters.get(1);
        else
            return parameters.get(2);
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s, %s)", getName(), "boolean", "if_true", "if_false");
    }
}
