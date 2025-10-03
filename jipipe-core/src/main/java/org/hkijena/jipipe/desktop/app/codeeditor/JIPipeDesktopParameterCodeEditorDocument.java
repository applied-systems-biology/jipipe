package org.hkijena.jipipe.desktop.app.codeeditor;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;

public class JIPipeDesktopParameterCodeEditorDocument implements JIPipeDesktopCodeEditorDocument{
    private final JIPipeParameterAccess parameterAccess;

    public JIPipeDesktopParameterCodeEditorDocument(JIPipeParameterAccess parameterAccess) {
        this.parameterAccess = parameterAccess;
    }

    public JIPipeParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    @Override
    public JIPipeScriptParameter pull() {
        return parameterAccess.get(JIPipeScriptParameter.class);
    }

    @Override
    public void push(JIPipeScriptParameter param) {
        parameterAccess.set(param);
    }
}
