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

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_dir", "Project directory", "The project directory (if available; will be the same as the data directory otherwise)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_data_dirs", "Project data directories", "The user-configured project data directories as map. Access entries by the key."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_data_dir.<key>", "Specific project data directory", "If the keyof a project data directory is a valid variable name (no spaces etc.), the directory can also be accessed by such variables."));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        HashSet<JIPipeExpressionParameterVariableInfo> result = new HashSet<>(VARIABLES);
        if(workbench.getProject() != null) {
            for (Map.Entry<String, Path> entry : workbench.getProject().getDirectoryMap().entrySet()) {
                if(JIPipeExpressionParameter.isValidVariableName(entry.getKey())) {
                    VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_data_dir." + entry.getKey(), "Project data directory '" + entry.getKey() + "'", "The user-configured project data directory '" + entry.getKey() + "'"));
                }
            }
        }
        return result;
    }
}
