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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SetJIPipeDocumentation(name = "Path is directory", description = "Returns true if the provided string is a path that points to a directory, otherwise false.")
public class IsDirectoryFunction extends ExpressionFunction {
    public IsDirectoryFunction() {
        super("IS_DIRECTORY", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String pathString = StringUtils.nullToEmpty(parameters.get(0));
        try {
            return Files.isDirectory(Paths.get(pathString));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Path", "String that contains the path", String.class);
        }
        return null;
    }
}
