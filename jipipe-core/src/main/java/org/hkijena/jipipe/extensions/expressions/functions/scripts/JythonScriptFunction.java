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

package org.hkijena.jipipe.extensions.expressions.functions.scripts;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.python.util.PythonInterpreter;

import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Jython (Python)", description = "Runs embedded Python (via the Jython interpreter)")
public class JythonScriptFunction extends ExpressionFunction {

    public JythonScriptFunction() {
        super("JYTHON", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("Script", "The Jython script", String.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String script = StringUtils.nullToEmpty(parameters.get(0));
        PythonInterpreter pythonInterpreter = new PythonInterpreter();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            pythonInterpreter.set(entry.getKey(), entry.getValue());
        }
        return pythonInterpreter.eval(script);
    }
}
