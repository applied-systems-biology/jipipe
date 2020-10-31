package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Invert", description = "If the parameter is an array, the order is reversed. If a string, the string is reversed. Numbers and booleans are negated.")
public class InvertFunction extends ExpressionFunction {

    public InvertFunction() {
        super("INVERT", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        Object value = parameters.get(0);
        if(value instanceof Collection) {
            ArrayList<?> list = new ArrayList<>((Collection<?>) value);
            Collections.reverse(list);
            return list;
        }
        else if(value instanceof Number) {
            return -((Number) value).doubleValue();
        }
        else if(value instanceof Boolean) {
            return !((boolean)value);
        }
        else {
            return StringUtils.reverse("" + value);
        }
    }
}
