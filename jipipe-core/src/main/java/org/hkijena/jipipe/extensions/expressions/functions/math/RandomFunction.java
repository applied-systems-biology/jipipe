package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;
import java.util.Random;

@SetJIPipeDocumentation(name = "Random number", description = "Calculates a random real number within the interval [0, 1). One argument (x): " +
        "the number will be between [0, x]. Two arguments (x, y): the number will be between [x, y).")
public class RandomFunction extends ExpressionFunction {

    public static final Random RANDOM = new Random();

    public RandomFunction() {
        super("RANDOM", 0, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("x", "If this is the only parameter, will result in a number within [0, x). If two parameters (x, y) are present, " +
                        "the number will be within [x, y)", Number.class);
            case 1:
                return new ParameterInfo("y", "The randomly generated number will be within [x, y)", Number.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double min = 0;
        double max = 1;
        if (parameters.size() == 1) {
            max = ((Number) parameters.get(0)).doubleValue();
        } else if (parameters.size() == 2) {
            min = ((Number) parameters.get(0)).doubleValue();
            max = ((Number) parameters.get(1)).doubleValue();
        }
        if (min > max) {
            double t = min;
            min = max;
            max = t;
        }
        return min + RANDOM.nextDouble() * (max - min);
    }
}
