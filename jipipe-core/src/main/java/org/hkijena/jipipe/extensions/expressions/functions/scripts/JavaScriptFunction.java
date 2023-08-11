package org.hkijena.jipipe.extensions.expressions.functions.scripts;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "JavaScript", description = "Runs embedded Javascript")
public class JavaScriptFunction extends ExpressionFunction {

    public JavaScriptFunction() {
        super("JS", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("Script", "The JavaScript", String.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String script = StringUtils.nullToEmpty(parameters.get(0));
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        Bindings bindings = engine.createBindings();
        bindings.putAll(variables);
        try {
            return engine.eval(script, bindings);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
