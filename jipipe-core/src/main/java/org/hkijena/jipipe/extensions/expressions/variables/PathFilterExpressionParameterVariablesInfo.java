/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.expressions.variables;

import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.nio.file.Path;
import java.util.Set;

/**
 * {@link ExpressionParameterVariablesInfo} that defines one variable 'x' that references a filesystem path or file name.
 */
public class PathFilterExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    public static final ExpressionParameterVariable VARIABLE_PATH = new ExpressionParameterVariable("Path", "The full path. Can be relative or absolute.", "path");
    public static final ExpressionParameterVariable VARIABLE_ABSPATH = new ExpressionParameterVariable("Absolute path", "The full absolute path.", "absolute_path");
    public static final ExpressionParameterVariable VARIABLE_NAME = new ExpressionParameterVariable("Name", "The file or directory name", "name");
    public static final ExpressionParameterVariable VARIABLE_PARENT = new ExpressionParameterVariable("Parent", "The parent directory", "parent");
    private static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = ImmutableSet.of(VARIABLE_NAME, VARIABLE_ABSPATH, VARIABLE_PARENT, VARIABLE_PATH);
    }

    public static void buildFor(Path path, ExpressionVariables result) {
        result.set("path", path.toString());
        result.set("absolute_path", path.toAbsolutePath().toString());
        result.set("name", path.getFileName().toString());
        result.set("parent", path.getParent().toString());
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
