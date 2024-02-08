package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.HashSet;
import java.util.Set;

public class JIPipeProjectDirectoriesVariablesInfo implements ExpressionParameterVariablesInfo {
    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_dir", "Project directory", "The project directory (if available; will be the same as the data directory otherwise)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("project_data_dirs", "Project data directories", "The user-configured project data directories as map. Access entries by the key."));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
