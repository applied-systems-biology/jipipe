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

package org.hkijena.jipipe.plugins.expressions.variables;

import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;

import java.nio.file.Path;
import java.util.Set;

/**
 * {@link ExpressionParameterVariablesInfo} that defines one variable 'x' that references a filesystem path or file name.
 */
public class PathFilterExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    public static final JIPipeExpressionParameterVariableInfo VARIABLE_PATH = new JIPipeExpressionParameterVariableInfo("path", "Path", "The full path. Can be relative or absolute.");
    public static final JIPipeExpressionParameterVariableInfo VARIABLE_ABSPATH = new JIPipeExpressionParameterVariableInfo("absolute_path", "Absolute path", "The full absolute path.");
    public static final JIPipeExpressionParameterVariableInfo VARIABLE_NAME = new JIPipeExpressionParameterVariableInfo("name", "Name", "The file or directory name");
    public static final JIPipeExpressionParameterVariableInfo VARIABLE_PARENT = new JIPipeExpressionParameterVariableInfo("parent", "Parent", "The parent directory");
    private static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = ImmutableSet.of(VARIABLE_NAME, VARIABLE_ABSPATH, VARIABLE_PARENT, VARIABLE_PATH);
    }

    public static void buildFor(Path path, JIPipeExpressionVariablesMap result) {
        result.set("path", path.toString());
        result.set("absolute_path", path.toAbsolutePath().toString());
        result.set("name", path.getFileName().toString());
        result.set("parent", path.getParent().toString());
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
