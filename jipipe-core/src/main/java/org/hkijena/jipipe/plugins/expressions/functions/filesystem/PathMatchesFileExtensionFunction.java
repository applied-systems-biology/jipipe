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

import java.util.List;
import java.util.Locale;

@SetJIPipeDocumentation(name = "Path matches file extension", description = "Returns true the provided path matches any of the file extensions. Case-insensitive. Extensions should start with a '.', e.g., '.png'")
public class PathMatchesFileExtensionFunction extends ExpressionFunction {
    public PathMatchesFileExtensionFunction() {
        super("PATH_MATCHES_EXTENSION", 2, Short.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String pathString = StringUtils.nullToEmpty(parameters.getFirst()).toLowerCase(Locale.ROOT);
        for (int i = 1; i < parameters.size(); i++) {
            String extensionString = StringUtils.nullToEmpty(parameters.get(i).toString()).toLowerCase(Locale.ROOT);
            if(pathString.endsWith(extensionString)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Path", "String that contains the path", String.class);
        }
        return  new ParameterInfo("Extension " + index, "The extension (including the .)", String.class);
    }
}
