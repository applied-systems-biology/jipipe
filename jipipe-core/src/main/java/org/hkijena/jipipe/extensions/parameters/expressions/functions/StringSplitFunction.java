package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Split string", description = "Splits a string by the right parameter into an array. For example you can split a string 'a_b_c' by '_' into 'a', 'b', and 'c'")
public class StringSplitFunction extends ExpressionFunction {

    public StringSplitFunction() {
        super("SPLIT_STRING", 2);
    }

    @Override
    public String getSignature() {
        return getName() + "(text, pattern)";
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        String text = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        return Arrays.asList(text.split(pattern));
    }
}
