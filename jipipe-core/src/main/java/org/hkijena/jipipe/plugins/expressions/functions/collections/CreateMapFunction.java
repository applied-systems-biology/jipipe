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

import java.util.*;

@SetJIPipeDocumentation(name = "Create map", description = "Creates a map from multiple pair arrays. Use the PAIR(key, value) function to create pairs. Duplicate keys are overwritten by newer values.")
public class CreateMapFunction extends ExpressionFunction {

    public CreateMapFunction() {
        super("MAP", 0, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Map<Object, Object> map = new HashMap<>();
        for (Object parameter : parameters) {
            Collection<?> pair = (Collection<?>) parameter;
            Iterator<?> iterator = pair.iterator();
            Object key = iterator.next();
            Object value = iterator.next();
            map.put(key, value);
        }
        return map;
    }
}
