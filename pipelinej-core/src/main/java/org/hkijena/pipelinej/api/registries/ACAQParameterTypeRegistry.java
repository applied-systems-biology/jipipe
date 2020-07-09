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

package org.hkijena.pipelinej.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.pipelinej.ACAQDefaultRegistry;
import org.hkijena.pipelinej.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Registry for all known parameter types
 */
public class ACAQParameterTypeRegistry {
    private BiMap<String, ACAQParameterTypeDeclaration> registeredParameters = HashBiMap.create();
    private BiMap<Class<?>, ACAQParameterTypeDeclaration> registeredParameterClasses = HashBiMap.create();

    /**
     * Registers a new parameter type
     *
     * @param parameterTypeDeclaration the parameter type
     */
    public void register(ACAQParameterTypeDeclaration parameterTypeDeclaration) {
        if (registeredParameters.containsKey(parameterTypeDeclaration.getId()))
            throw new RuntimeException("Parameter type with ID '" + parameterTypeDeclaration.getId() + "' already exists!");
        if (registeredParameterClasses.containsKey(parameterTypeDeclaration.getFieldClass()))
            throw new RuntimeException("Parameter type with class '" + parameterTypeDeclaration.getFieldClass() + "' already exists!");
        registeredParameters.put(parameterTypeDeclaration.getId(), parameterTypeDeclaration);
        registeredParameterClasses.put(parameterTypeDeclaration.getFieldClass(), parameterTypeDeclaration);
    }

    public BiMap<String, ACAQParameterTypeDeclaration> getRegisteredParameters() {
        return ImmutableBiMap.copyOf(registeredParameters);
    }

    /**
     * Returns a parameter type declaration by ID
     *
     * @param id the ID
     * @return the declaration
     */
    public ACAQParameterTypeDeclaration getDeclarationById(String id) {
        return registeredParameters.get(id);
    }

    /**
     * Return a parameter type by its field class
     *
     * @param fieldClass the field class
     * @return the declaration
     */
    public ACAQParameterTypeDeclaration getDeclarationByFieldClass(Class<?> fieldClass) {
        return registeredParameterClasses.get(fieldClass);
    }

    public static ACAQParameterTypeRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getParameterTypeRegistry();
    }
}
