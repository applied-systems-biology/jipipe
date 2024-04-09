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
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SetJIPipeDocumentation(name = "Get parent directory", description = "Returns the Nth (default 1) parent directory of the provided file/folder path.")
public class GetParentDirectoryFunction extends ExpressionFunction {
    public GetParentDirectoryFunction() {
        super("GET_PARENT_DIRECTORY", 1, 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String pathString = StringUtils.nullToEmpty(parameters.get(0));
        int N = parameters.size() > 1 ? (int) StringUtils.parseDouble(StringUtils.orElse(parameters.get(1), "1")) : 1;
        Path path = Paths.get(pathString);
        for (int i = 0; i < N; i++) {
            path = path.getParent();
        }
        return path.toString();
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Path", "String that contains the path", String.class);
        } else if (index == 1) {
            return new ParameterInfo("N", "Selects the Nth parent", Number.class);
        }
        return null;
    }
}
