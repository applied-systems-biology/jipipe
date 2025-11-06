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

package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JIPipeProjectDirectoriesVariablesInfo implements JIPipeExpressionVariablesInfo {
    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_dir",
                "Project directory",
                "The project directory (if available; will be the same as the data directory otherwise)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_data_dirs",
                "Project user paths",
                "The user-configured project paths as map. Access entries by the key. Equal to project_user_paths."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_user_paths",
                "Project user paths",
                "The user-configured project paths as map. Access entries by the key. Equal to project_data_dirs."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_data_dir.<key>",
                "Specific project user path",
                "If the key of a project user path is a valid variable name (no spaces etc.), the path can also be accessed by such variables. Equal to project_data_dir.<key>."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_user_path.<key>",
                "Specific project user path",
                "If the key of a project user path is a valid variable name (no spaces etc.), the path can also be accessed by such variables. Equal to project_user_path.<key>."));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        HashSet<JIPipeExpressionParameterVariableInfo> result = new HashSet<>(VARIABLES);
        if (workbench.getProject() != null) {
            for (Map.Entry<String, Path> entry : workbench.getProject().getDirectoryMap().entrySet()) {
                if (JIPipeExpressionParameter.isValidVariableName(entry.getKey())) {
                    VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_data_dir." + entry.getKey(), "Project user path '" + entry.getKey() + "'", "The user-configured project user path '" + entry.getKey() + "'"));
                    VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_user_path." + entry.getKey(), "Project user path '" + entry.getKey() + "'", "The user-configured project user path '" + entry.getKey() + "'"));
                }
            }
        }
        return result;
    }
}
