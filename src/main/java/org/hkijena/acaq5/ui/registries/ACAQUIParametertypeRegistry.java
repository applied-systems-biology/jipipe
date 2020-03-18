package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQRegistryService;
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
        Class<? extends ACAQParameterEditorUI> uiClass = parameterTypes.getOrDefault(parameterAccess.getFieldClass(), null);
        if(uiClass == null) {
            // Search a matching one
            for (Map.Entry<Class<?>, Class<? extends ACAQParameterEditorUI>> entry : parameterTypes.entrySet()) {
                if(entry.getKey().isAssignableFrom(parameterAccess.getFieldClass())) {
                    uiClass = entry.getValue();
                    break;
                }
            }
        }
        if(uiClass == null) {
            throw new NullPointerException("Could not find parameter editor for parameter class '" + parameterAccess.getFieldClass() + "'");
        }
        try {
            return uiClass.getConstructor(ACAQWorkbenchUI.class, ACAQParameterAccess.class).newInstance(workbenchUI, parameterAccess);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static ACAQUIParametertypeRegistry getInstance() {
        return ACAQRegistryService.getInstance().getUIParametertypeRegistry();
    }
}
