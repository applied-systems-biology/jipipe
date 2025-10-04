package org.hkijena.jipipe.plugins.parameters.ui.api.script;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.plugins.parameters.ui.api.JIPipeDesktopScriptParameterEditorUI;

import java.lang.ref.WeakReference;
import java.util.Objects;

public abstract class JIPipeDesktopScriptParameterEditorUIExternalEditor implements JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;
    private final WeakReference<JIPipeParameterCollection> parameterCollection;
    private final String parameterKey;
    private final JIPipeParameterTree parameterTree;
    private JIPipeParameterAccess parameterAccess;

    public JIPipeDesktopScriptParameterEditorUIExternalEditor(JIPipeDesktopWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        this.workbench = workbench;
        this.parameterCollection = parameterCollection;
        this.parameterKey = parameterKey;
        this.parameterTree = parameterTree;
        this.parameterAccess = parameterAccess;
    }

    public JIPipeParameterTree getParameterTree() {
        return parameterTree;
    }

    public WeakReference<JIPipeParameterCollection> getParameterCollection() {
        return parameterCollection;
    }

    public String getParameterKey() {
        return parameterKey;
    }

    public JIPipeParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    public boolean accessEquals(JIPipeParameterAccess access) {
        return this.parameterAccess == access || (access.getSource() == parameterCollection.get() && Objects.equals(access.getKey(), parameterKey));
    }

    public <T> T getParameter(Class<T> klass) {
        return parameterAccess.get(klass);
    }

    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public void close() {
        JIPipeDesktopScriptParameterEditorUI.OPENED_EXTERNAL_EDITORS.remove(this);
        parameterAccess = null;
    }


}
