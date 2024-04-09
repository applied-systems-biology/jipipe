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

package org.hkijena.jipipe.plugins.expressions.functions.filesystem;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Combine paths", description = "Combines all parameters into a path.")
public class PathCombineFunction extends ExpressionFunction {
    public PathCombineFunction() {
        super("PATH_COMBINE", 1, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        List<String> items = new ArrayList<>();
        for (Object parameter : parameters) {
            String item = StringUtils.nullToEmpty(parameter);
            items.add(item);
        }
        return Paths.get(String.join("/", items));
    }
}
