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
import org.hkijena.jipipe.JIPipeDefaultRegistry;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;

/**
 * Registry for all known parameter types
 */
public class JIPipeParameterTypeRegistry {
    private BiMap<String, JIPipeParameterTypeInfo> registeredParameters = HashBiMap.create();
    private BiMap<Class<?>, JIPipeParameterTypeInfo> registeredParameterClasses = HashBiMap.create();

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

    public static JIPipeParameterTypeRegistry getInstance() {
        return JIPipeDefaultRegistry.getInstance().getParameterTypeRegistry();
    }
}
