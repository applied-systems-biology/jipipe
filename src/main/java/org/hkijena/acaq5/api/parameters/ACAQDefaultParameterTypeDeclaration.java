package org.hkijena.acaq5.api.parameters;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Standard implementation of {@link ACAQParameterTypeDeclaration}
 */
public class ACAQDefaultParameterTypeDeclaration implements ACAQParameterTypeDeclaration {

    private String id;
    private Class<?> fieldClass;
    private Supplier<Object> newInstanceGenerator;
    private Function<Object, Object> duplicateFunction;
    private String name;
    private String description;


    /**
     * @param id                   the unique ID
     * @param fieldClass           the parameter class
     * @param newInstanceGenerator a function that generates a new instance
     * @param duplicateFunction    a function that creates a deep copy
     * @param name                 the name
     * @param description          the description
     */
    public ACAQDefaultParameterTypeDeclaration(String id, Class<?> fieldClass, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description) {
        this.id = id;
        this.fieldClass = fieldClass;
        this.newInstanceGenerator = newInstanceGenerator;
        this.duplicateFunction = duplicateFunction;
        this.name = name;
        this.description = description;
    }

    @Override
    public Object newInstance() {
        return newInstanceGenerator.get();
    }

    @Override
    public Object duplicate(Object original) {
        return duplicateFunction.apply(original);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<?> getFieldClass() {
        return fieldClass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
