package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.scijava.Context;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ACAQUIParametertypeRegistry {

    private Map<Class<?>, ACAQDocumentation> parameterDocumentations = new HashMap<>();
    private Map<Class<?>, Class<? extends ACAQParameterEditorUI>> parameterTypes = new HashMap<>();

    public ACAQUIParametertypeRegistry() {

    }

    public void registerParameterEditor(Class<?> parameterType, Class<? extends ACAQParameterEditorUI> uiClass) {
        parameterTypes.put(parameterType, uiClass);
    }

    public void registerDocumentation(Class<?> parameterType, ACAQDocumentation documentation) {
        parameterDocumentations.put(parameterType, documentation);
    }

    public ACAQDocumentation getDocumentationFor(Class<?> parameterType) {
        return parameterDocumentations.getOrDefault(parameterType, null);
    }

    public ACAQParameterEditorUI createEditorFor(Context context, ACAQParameterAccess parameterAccess) {
        Class<? extends ACAQParameterEditorUI> uiClass = parameterTypes.getOrDefault(parameterAccess.getFieldClass(), null);
        if (uiClass == null) {
            // Search a matching one
            for (Map.Entry<Class<?>, Class<? extends ACAQParameterEditorUI>> entry : parameterTypes.entrySet()) {
                if (entry.getKey().isAssignableFrom(parameterAccess.getFieldClass())) {
                    uiClass = entry.getValue();
                    break;
                }
            }
        }
        if (uiClass == null) {
            throw new NullPointerException("Could not find parameter editor for parameter class '" + parameterAccess.getFieldClass() + "'");
        }
        try {
            return uiClass.getConstructor(Context.class, ACAQParameterAccess.class).newInstance(context, parameterAccess);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static ACAQUIParametertypeRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUIParametertypeRegistry();
    }
}
