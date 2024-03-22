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

package org.hkijena.jipipe.extensions.utils.algorithms.processes;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;

import java.util.HashSet;
import java.util.Set;

public class ProcessEnvironmentVariablesInfo implements ExpressionParameterVariablesInfo {
    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
        result.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
        result.add(new JIPipeExpressionParameterVariableInfo("executable", "Executable", "The executable"));
        result.add(new JIPipeExpressionParameterVariableInfo("input_folder", "Input data folder", "Points to the directory where input data is stored. Contains subdirectories named according to the input slots that follow the JIPipe data format."));
        result.add(new JIPipeExpressionParameterVariableInfo("output_folder", "Output data folder", "Points to the directory where input data is stored. Contains subdirectories named according to the input slots that follow the JIPipe data format."));
        return result;
    }
}
