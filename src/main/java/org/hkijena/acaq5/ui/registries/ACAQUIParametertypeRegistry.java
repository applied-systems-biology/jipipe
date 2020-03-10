package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ACAQUIParametertypeRegistry {

    private Map<Class<?>, Class<? extends ACAQParameterEditorUI>> parameterTypes = new HashMap<>();

    public ACAQUIParametertypeRegistry() {

    }

    public void registerParameterEditor(Class<?> parameterType, Class<? extends ACAQParameterEditorUI> uiClass) {
        parameterTypes.put(parameterType, uiClass);
    }

    public ACAQParameterEditorUI createEditorFor(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        Class<? extends ACAQParameterEditorUI> uiClass = parameterTypes.get(parameterAccess.getFieldClass());
        try {
            return uiClass.getConstructor(ACAQWorkbenchUI.class, ACAQParameterAccess.class).newInstance(workbenchUI, parameterAccess);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
