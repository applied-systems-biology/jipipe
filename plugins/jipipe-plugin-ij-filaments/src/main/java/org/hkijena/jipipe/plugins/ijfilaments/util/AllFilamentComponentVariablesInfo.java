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

package org.hkijena.jipipe.plugins.ijfilaments.util;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class AllFilamentComponentVariablesInfo implements JIPipeExpressionVariablesInfo {

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> VARIABLES = new HashSet<>();
        FilamentComponentVariablesInfo filamentComponentVariablesInfo = new FilamentComponentVariablesInfo();
        for (JIPipeExpressionParameterVariableInfo variable : filamentComponentVariablesInfo.getVariables(workbench, null, null)) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("all." + variable.getKey(), variable.getName() + " (all values)", "Array of all measurement values. " + variable.getDescription()));
        }
        return VARIABLES;
    }
}
