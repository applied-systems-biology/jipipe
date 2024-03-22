/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterGenerator;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Registry for all known parameter types
 */
public class JIPipeParameterTypeRegistry {
    private final BiMap<String, JIPipeParameterTypeInfo> registeredParameters = HashBiMap.create();
    private final BiMap<Class<?>, JIPipeParameterTypeInfo> registeredParameterClasses = HashBiMap.create();
    private final Map<Class<?>, Class<? extends JIPipeDesktopParameterEditorUI>> parameterTypesUIs = new HashMap<>();
    private final Map<Class<?>, Set<JIPipeParameterGenerator>> parameterGeneratorUIs = new HashMap<>();
    private final JIPipe jiPipe;

    public JIPipeParameterTypeRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

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
        getJIPipe().getProgressInfo().log("Registered parameter type id=" + info.getId() + " of type " + info.getFieldClass());
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
    public void registerParameterEditor(Class<?> parameterType, Class<? extends JIPipeDesktopParameterEditorUI> uiClass) {
        parameterTypesUIs.put(parameterType, uiClass);
    }

    /**
     * Creates editor for the parameter
     *
     * @param workbench       SciJava context
     * @param parameterTree   the parameter tree
     * @param parameterAccess the parameter
     * @return Parameter editor UI
     */
    public JIPipeDesktopParameterEditorUI createEditorFor(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Class<? extends JIPipeDesktopParameterEditorUI> uiClass = parameterTypesUIs.getOrDefault(parameterAccess.getFieldClass(), null);
        if (uiClass == null) {
            // Search a matching one
            for (Map.Entry<Class<?>, Class<? extends JIPipeDesktopParameterEditorUI>> entry : parameterTypesUIs.entrySet()) {
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
            return uiClass.getConstructor(JIPipeWorkbench.class, JIPipeParameterTree.class, JIPipeParameterAccess.class).newInstance(workbench, parameterTree, parameterAccess);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
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
     * @param generator      The generator object
     */
    public void registerGenerator(Class<?> parameterClass, JIPipeParameterGenerator generator) {
        Set<JIPipeParameterGenerator> generators = parameterGeneratorUIs.getOrDefault(parameterClass, null);
        if (generators == null) {
            generators = new HashSet<>();
            parameterGeneratorUIs.put(parameterClass, generators);
        }
        generators.add(generator);
    }

    /**
     * Returns all generators for the parameter class
     *
     * @param parameterClass the parameter class
     * @return Set of generators
     */
    public Set<JIPipeParameterGenerator> getGeneratorsFor(Class<?> parameterClass) {
        return parameterGeneratorUIs.getOrDefault(parameterClass, Collections.emptySet());
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }
}
