package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Join string array", description = "Given an array of strings, the array is joined into one string with a delimiter. " +
        "For example you can join 'a', 'b' and 'c' into 'a,b,c'")
public class StringJoinFunction extends ExpressionFunction {

    public StringJoinFunction() {
        super("JOIN_STRING", 1, 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        List<String> strings = new ArrayList<>();
        for (Object item : (Collection<?>) parameters.get(0)) {
            strings.add("" + item);
        }
        String delimiter = parameters.size() > 1 ? ("" + parameters.get(1)) : "";
        return String.join(delimiter, strings);
    }
}
