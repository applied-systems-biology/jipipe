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

package org.hkijena.jipipe.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.jipipe.api.JIPipeDefaultDocumentation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
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
 * Registry for all known parameter types
 */
public class JIPipeParameterTypeRegistry {
    private BiMap<String, JIPipeParameterTypeInfo> registeredParameters = HashBiMap.create();
    private BiMap<Class<?>, JIPipeParameterTypeInfo> registeredParameterClasses = HashBiMap.create();
    private Map<Class<?>, Class<? extends JIPipeParameterEditorUI>> parameterTypesUIs = new HashMap<>();
    private Map<Class<?>, Set<Class<? extends JIPipeParameterGeneratorUI>>> parameterGeneratorUIs = new HashMap<>();
    private Map<Class<? extends JIPipeParameterGeneratorUI>, JIPipeDocumentation> parameterGeneratorUIDocumentations = new HashMap<>();

    /**
     * Registers a new parameter type
     *
     * @param info the parameter type
     */
    public void register(JIPipeParameterTypeInfo info) {
        if (registeredParameters.containsKey(info.getId()))
            throw new RuntimeException("Parameter type with ID '" + info.getId() + "' already exists!");
        if (registeredParameterClasses.containsKey(info.getFieldClass()))
            throw new RuntimeException("Parameter type with class '" + info.getFieldClass() + "' already exists!");
        registeredParameters.put(info.getId(), info);
        registeredParameterClasses.put(info.getFieldClass(), info);
    }

    public BiMap<String, JIPipeParameterTypeInfo> getRegisteredParameters() {
        return ImmutableBiMap.copyOf(registeredParameters);
    }

    /**
     * Returns a parameter type info by ID
     *
     * @param id the ID
     * @return the info
     */
    public JIPipeParameterTypeInfo getInfoById(String id) {
        return registeredParameters.get(id);
    }

    /**
     * Return a parameter type by its field class
     *
     * @param fieldClass the field class
     * @return the info
     */
    public JIPipeParameterTypeInfo getInfoByFieldClass(Class<?> fieldClass) {
        return registeredParameterClasses.get(fieldClass);
    }

    /**
     * Registers a new parameter type
     *
     * @param parameterType parameter type
     * @param uiClass       corresponding editor UI
     */
    public void registerParameterEditor(Class<?> parameterType, Class<? extends JIPipeParameterEditorUI> uiClass) {
        parameterTypesUIs.put(parameterType, uiClass);
    }

    /**
     * Creates editor for the parameter
     *
     * @param workbench       SciJava context
     * @param parameterAccess the parameter
     * @return Parameter editor UI
     */
    public JIPipeParameterEditorUI createEditorFor(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        Class<? extends JIPipeParameterEditorUI> uiClass = parameterTypesUIs.getOrDefault(parameterAccess.getFieldClass(), null);
        if (uiClass == null) {
            // Search a matching one
            for (Map.Entry<Class<?>, Class<? extends JIPipeParameterEditorUI>> entry : parameterTypesUIs.entrySet()) {
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
        return parameterTypesUIs.containsKey(parameterType);
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
        Set<Class<? extends JIPipeParameterGeneratorUI>> generators = parameterGeneratorUIs.getOrDefault(parameterClass, null);
        if (generators == null) {
            generators = new HashSet<>();
            parameterGeneratorUIs.put(parameterClass, generators);
        }
        generators.add(uiClass);
        parameterGeneratorUIDocumentations.put(uiClass, new JIPipeDefaultDocumentation(name, description));
    }

    /**
     * Returns all generators for the parameter class
     *
     * @param parameterClass the parameter class
     * @return Set of generators
     */
    public Set<Class<? extends JIPipeParameterGeneratorUI>> getGeneratorsFor(Class<?> parameterClass) {
        return parameterGeneratorUIs.getOrDefault(parameterClass, Collections.emptySet());
    }

    /**
     * Returns documentation for the generator
     *
     * @param generatorClass the generator
     * @return documentation
     */
    public JIPipeDocumentation getGeneratorDocumentationFor(Class<? extends JIPipeParameterGeneratorUI> generatorClass) {
        return parameterGeneratorUIDocumentations.get(generatorClass);
    }
}
