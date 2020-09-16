/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.registries;

import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterGeneratorUI;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry for parameter types
 */
public class JIPipeUIParameterTypeRegistry {
    private Map<Class<?>, Class<? extends JIPipeParameterEditorUI>> parameterTypes = new HashMap<>();

    private Map<Class<?>, Set<Class<? extends JIPipeParameterGeneratorUI>>> parameterGenerators = new HashMap<>();
    private Map<Class<? extends JIPipeParameterGeneratorUI>, JIPipeDocumentation> parameterGeneratorDocumentations = new HashMap<>();

    /**
     * New instance
     */
    public JIPipeUIParameterTypeRegistry() {

    }

    /**
     * Registers a new parameter type
     *
     * @param parameterType parameter type
     * @param uiClass       corresponding editor UI
     */
    public void registerParameterEditor(Class<?> parameterType, Class<? extends JIPipeParameterEditorUI> uiClass) {
        parameterTypes.put(parameterType, uiClass);
    }

    /**
     * Creates editor for the parameter
     *
     * @param workbench       SciJava context
     * @param parameterAccess the parameter
     * @return Parameter editor UI
     */
    public JIPipeParameterEditorUI createEditorFor(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        Class<? extends JIPipeParameterEditorUI> uiClass = parameterTypes.getOrDefault(parameterAccess.getFieldClass(), null);
        if (uiClass == null) {
            // Search a matching one
            for (Map.Entry<Class<?>, Class<? extends JIPipeParameterEditorUI>> entry : parameterTypes.entrySet()) {
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
            return uiClass.getConstructor(JIPipeWorkbench.class, JIPipeParameterAccess.class).newInstance(workbench, parameterAccess);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if there is an editor for the parameter
     *
     * @param parameterType the parameter type
     * @return if there is an editor for the parameter
     */
    public boolean hasEditorFor(Class<?> parameterType) {
        return parameterTypes.containsKey(parameterType);
    }

    /**
     * Registers a UI that can generate parameters
     *
     * @param parameterClass Parameter class
     * @param uiClass        The generator UI class
     * @param name           Generator name
     * @param description    Description for the generator
     */
    public void registerGenerator(Class<?> parameterClass, Class<? extends JIPipeParameterGeneratorUI> uiClass, String name, String description) {
        Set<Class<? extends JIPipeParameterGeneratorUI>> generators = parameterGenerators.getOrDefault(parameterClass, null);
        if (generators == null) {
            generators = new HashSet<>();
            parameterGenerators.put(parameterClass, generators);
        }
        generators.add(uiClass);
        parameterGeneratorDocumentations.put(uiClass, new JIPipeDefaultDocumentation(name, description));
    }

    /**
     * Returns all generators for the parameter class
     *
     * @param parameterClass the parameter class
     * @return Set of generators
     */
    public Set<Class<? extends JIPipeParameterGeneratorUI>> getGeneratorsFor(Class<?> parameterClass) {
        return parameterGenerators.getOrDefault(parameterClass, Collections.emptySet());
    }

    /**
     * Returns documentation for the generator
     *
     * @param generatorClass the generator
     * @return documentation
     */
    public JIPipeDocumentation getGeneratorDocumentationFor(Class<? extends JIPipeParameterGeneratorUI> generatorClass) {
        return parameterGeneratorDocumentations.get(generatorClass);
    }

    public static JIPipeUIParameterTypeRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getUIParameterTypeRegistry();
    }
}
