package org.hkijena.acaq5.api.registries;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Registry for all known parameter types
 */
public class ACAQParameterTypeRegistry {
    private BiMap<String, ACAQParameterTypeDeclaration> registeredParameters = HashBiMap.create();
    private BiMap<Class<?>, ACAQParameterTypeDeclaration> registeredParameterClasses = HashBiMap.create();

    public static ACAQParameterTypeRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getParameterTypeRegistry();
    }

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
}
