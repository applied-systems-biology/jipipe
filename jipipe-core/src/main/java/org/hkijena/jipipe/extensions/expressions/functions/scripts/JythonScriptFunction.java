package org.hkijena.jipipe.extensions.expressions.functions.scripts;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;
import org.python.util.PythonInterpreter;

import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Jython (Python)", description = "Runs embedded Python (via the Jython interpreter)")
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
