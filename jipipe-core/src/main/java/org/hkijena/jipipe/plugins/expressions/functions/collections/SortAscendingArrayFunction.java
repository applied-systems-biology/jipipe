/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.expressions.functions.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Sort array (ascending)", description = "Sorts an array in ascending order")
public class SortAscendingArrayFunction extends ExpressionFunction {

    public SortAscendingArrayFunction() {
        super("SORT_ASCENDING", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("array", "The array", Collection.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return ((Collection) parameters.get(0)).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }
}
