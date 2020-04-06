package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.scijava.Context;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for parameter types
 */
public class ACAQUIParametertypeRegistry {

    private Map<Class<?>, ACAQDocumentation> parameterDocumentations = new HashMap<>();
    private Map<Class<?>, Class<? extends ACAQParameterEditorUI>> parameterTypes = new HashMap<>();

    /**
     * New instance
     */
    public ACAQUIParametertypeRegistry() {

    }

    /**
     * Registers a new parameter type
     *
     * @param parameterType parameter type
     * @param uiClass       corresponding editor UI
     */
    public void registerParameterEditor(Class<?> parameterType, Class<? extends ACAQParameterEditorUI> uiClass) {
        parameterTypes.put(parameterType, uiClass);
    }

    /**
     * Registers documentation for a parameter type
     *
     * @param parameterType parameter type
     * @param documentation the documentation
     */
    public void registerDocumentation(Class<?> parameterType, ACAQDocumentation documentation) {
        parameterDocumentations.put(parameterType, documentation);
    }

    /**
     * Gets documentation for a parameter type
     *
     * @param parameterType parameter type
     * @return documentation. Can be null.
     */
    public ACAQDocumentation getDocumentationFor(Class<?> parameterType) {
        return parameterDocumentations.getOrDefault(parameterType, null);
    }

    /**
     * Creates editor for the parameter
     *
     * @param context         SciJava context
     * @param parameterAccess the parameter
     * @return Parameter editor UI
     */
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

    /**
     * Returns true if there is an editor for the parameter
     * @param parameterType the parameter type
     * @return if there is an editor for the parameter
     */
    public boolean hasEditorFor(Class<?> parameterType) {
        return parameterTypes.containsKey(parameterType);
    }

    public static ACAQUIParametertypeRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUIParametertypeRegistry();
    }
}
